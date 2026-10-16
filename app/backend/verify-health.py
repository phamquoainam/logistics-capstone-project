"""Actual HTTP probes against owned PG18; reuse existing isolated wire provisioning."""
import json
from pathlib import Path
import re
import runpy
import subprocess
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
HARNESS = runpy.run_path(str(ROOT / 'packages/api-client/scripts/wire-harness.py'))
OUTPUT = ROOT / '.demo/d1-08-health' / ('runtime-' + uuid.uuid4().hex[:12] + '.json')


def verify(context_path, env):
    context = json.loads(Path(context_path).read_text(encoding='utf-8'))
    state = json.loads(Path(context['statePath']).read_text(encoding='utf-8'))
    base = context['baseUrl'].removesuffix('/api/demo/v1')
    records = []
    paused = False

    def request(path, expected, body=None):
        start = time.monotonic()
        try:
            with urllib.request.urlopen(base + path, timeout=8) as response:
                status, raw = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, raw = error.code, error.read()
        actual = json.loads(raw)
        assert status in (expected if isinstance(expected, tuple) else (expected,)), 'Unexpected health/security HTTP status'
        if body is not None:
            assert actual == body, 'Health must expose exactly the public status'
        if path in ('/actuator/health/liveness', '/actuator/health/readiness'):
            assert set(actual) == {'status'} and actual['status'] in ('UP', 'DOWN', 'OUT_OF_SERVICE')
        for secret in (context['password'], context['tokenSecret'], env['PG_RUNTIME_PASSWORD'], env['PG_MIGRATOR_PASSWORD']):
            assert secret not in raw.decode(), 'Health response credential leak'
        assert not re.search(rb'jdbc:|exception|stackTrace|components|details|database|Hikari', raw, re.I)
        record = dict(path=path, status=status, elapsedMs=round((time.monotonic() - start) * 1000))
        if body is not None: record['body'] = actual
        records.append(record)
        return record

    try:
        request('/actuator/health/liveness', 200, {'status': 'UP'})
        request('/actuator/health/readiness', 200, {'status': 'UP'})
        request('/api/demo/v1/health', 200, {'status': 'UP', 'scope': 'local-demo'})
        for path in ('/actuator', '/actuator/health', '/actuator/health/db',
                     '/actuator/health/readiness/db', '/actuator/env', '/actuator/beans', '/actuator/metrics'):
            request(path, 401)
        HARNESS['owned'](state)
        HARNESS['docker']('pause', state['id'])
        paused = True
        assert HARNESS['owned'](state)['State']['Paused'], 'Owned PG must actually be paused'
        for _ in range(2):
            result = request('/actuator/health/readiness', 503, {'status': 'DOWN'})
            assert result['elapsedMs'] < 8000, 'DB timeout exceeded HTTP observation budget'
            request('/actuator/health/liveness', 200, {'status': 'UP'})
        HARNESS['docker']('unpause', state['id'])
        paused = False
        recovery_start = time.monotonic()
        for attempt in range(20):
            result = request('/actuator/health/readiness', (200, 503))
            if result['status'] == 200:
                break
            if attempt == 19: raise AssertionError('Readiness did not recover')
            time.sleep(1)
        request('/actuator/health/liveness', 200, {'status': 'UP'})
        assert HARNESS['sql'](state, 'SELECT count(*) FROM identity.demo_account;') == '5'
        result = dict(status='PASS',postgresVersion=context['postgresVersion'], database=context['database'],
            backendPort=context['backendPort'], postgresPort=context['postgresPort'], migrations=context['migrations'],
            harnessDirectory=str(Path(context['directory']).relative_to(ROOT)),
            outage='pause/unpause same owned tmpfs container; real JDBC read/validation timeout',
            backendRestarted=False,recoveryMs=round((time.monotonic()-recovery_start)*1000),checks=records,
            limitations='Runtime recovery only; Flyway startup recovery and arbitrary network failure durations are not claimed')
        OUTPUT.parent.mkdir(parents=True,exist_ok=True)
        OUTPUT.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        print('Actual PG health UP -> DOWN -> RECOVERY PASS; status-only responses, same backend process',flush=True)
        print('Safe health evidence: '+str(OUTPUT),flush=True)
        return subprocess.CompletedProcess(args=['health-runtime-checks'], returncode=0)
    finally:
        if paused:
            HARNESS['owned'](state)
            HARNESS['docker']('unpause', state['id'])


if __name__ == '__main__':
    try:
        code = HARNESS['run'](verify)
        if OUTPUT.exists():
            result = json.loads(OUTPUT.read_text(encoding='utf-8'))
            directory = ROOT / result['harnessDirectory']
            result['harnessExit'] = code
            result['cleanup'] = json.loads((directory / 'safe-cleanup.json').read_text(encoding='utf-8'))
            result['backendLogPrivacy'] = json.loads((directory / 'safe-environment.json').read_text(encoding='utf-8'))['logPrivacy']
            assert code == 0 and all(result['cleanup'].values()) and result['backendLogPrivacy'] == 'PASS'
            OUTPUT.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        raise SystemExit(code)
    except (AssertionError, RuntimeError, OSError, KeyError, ValueError):
        print('FAIL/BLOCKED: health runtime verification; private diagnostics withheld')
        raise SystemExit(1) from None
