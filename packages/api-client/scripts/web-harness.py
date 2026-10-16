"""C-04 browser consumer of the existing owned PG/backend harness."""
import json
from pathlib import Path
import runpy
import socket
import subprocess
import sys
import time
import urllib.request

HARNESS = runpy.run_path(str(Path(__file__).with_name('wire-harness.py')))
ROOT = HARNESS['ROOT']
WEB = ROOT / 'app/frontend/web'


def run_web(context_path, env):
    context = json.loads(context_path.read_text(encoding='utf-8'))
    directory = Path(context['directory'])
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        port = listener.getsockname()[1]
    env = dict(env, C04_CONTEXT=str(context_path), C04_WEB_URL=f'http://127.0.0.1:{port}',
               C04_WEB_PORT=str(port), C04_BACKEND_ORIGIN=context['baseUrl'].removesuffix('/api/demo/v1'),
               C04_RESULT_DIR=str(directory / 'browser-results'), C04_SAFE_REPORT=str(directory / 'safe-browser-results.json'))
    # Do not let Playwright attach request bodies, credentials or traces on failure.
    env.pop('DEBUG', None)
    env.pop('PWDEBUG', None)
    vite = None
    stopped = False
    with (directory / 'vite-private.log').open('wb') as log:
        try:
            vite = subprocess.Popen(['node', 'node_modules/vite/bin/vite.js', '--config', 'vite.c04.config.ts', '--host', '127.0.0.1'],
                                    cwd=WEB, env=env, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT)
            for attempt in range(45):
                if vite.poll() is not None:
                    raise RuntimeError('Isolated Vite failed; private diagnostics suppressed')
                try:
                    with urllib.request.urlopen(env['C04_WEB_URL'], timeout=2) as response:
                        if response.status == 200:
                            break
                except OSError:
                    if attempt == 44:
                        raise RuntimeError('Isolated Vite readiness timeout') from None
                    time.sleep(1)
            result = subprocess.run(['node', 'node_modules/@playwright/test/cli.js', 'test'], cwd=WEB, env=env,
                                    stdin=subprocess.DEVNULL, timeout=300)
            return result
        finally:
            if vite:
                vite.terminate()
                try:
                    vite.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    vite.kill()
                    vite.wait(timeout=10)
                stopped = vite.poll() is not None
            HARNESS['private_json'](directory / 'safe-web-environment.json',
                dict(webPort=port, backendPort=context['backendPort'], viteStopped=stopped, realBackend=True))
            print('Owned Vite process stopped: ' + str(stopped), flush=True)


if __name__ == '__main__':
    try:
        raise SystemExit(HARNESS['run'](run_web))
    except (RuntimeError, OSError, KeyError, ValueError, subprocess.SubprocessError):
        print('FAIL: C-04 owned web harness; secret-bearing diagnostics suppressed', file=sys.stderr)
        raise SystemExit(1) from None
