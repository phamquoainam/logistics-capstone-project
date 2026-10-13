"""Local API/persistence proof; reads ignored credentials and never prints tokens/passwords."""
import argparse
import json
import uuid
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError

root = Path(__file__).resolve().parents[2]
settings = json.loads((root / '.demo/settings.json').read_text(encoding='utf-8-sig'))
proof_path = root / '.demo/evidence/persistence-proof.json'
base = 'http://127.0.0.1:8080/api/demo/v1'

def call(path, token=None, body=None, key=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    if key: headers['Idempotency-Key'] = key
    request = Request(base + path, data=None if body is None else json.dumps(body).encode(), headers=headers)
    try:
        with urlopen(request, timeout=15) as response:
            status, result = response.status, json.load(response)
    except HTTPError as error:
        status, result = error.code, json.load(error)
    assert status == expected, f'{path}: expected {expected}, received {status}'
    return result

def login(username, password):
    return call('/auth/login', body={'username': username, 'password': password})['accessToken']

parser = argparse.ArgumentParser()
parser.add_argument('mode', choices=['create', 'verify'])
mode = parser.parse_args().mode
assert call('/health')['status'] == 'UP'
a = login('demo.merchant.a', settings['merchantAPassword'])
b = login('demo.merchant.b', settings['merchantBPassword'])
admin = login('demo.admin', settings['adminPassword'])
call('/auth/me', expected=401)
call('/admin/users', a, expected=403)
assert len(call('/admin/users', admin)) == 5
if mode == 'create':
    key = str(uuid.uuid4())
    body = dict(reference='PERSIST-' + uuid.uuid4().hex[:12], pickupAddress='Synthetic demo pickup', recipientName='Demo recipient', recipientPhone='0000000000', deliveryStreet='Demo street', deliveryWard='Demo ward', deliveryProvince='Demo province', parcelDescription='Persistence demonstration', weightKg=1.25)
    created = call('/shipments', a, body, key, 201)
    replay = call('/shipments', a, body, key, 201)
    assert created['id'] == replay['id']
    proof = {'id': created['id'], 'reference': created['reference'], 'createdAt': created['createdAt'], 'key': key, 'body': body}
    proof_path.write_text(json.dumps(proof, indent=2), encoding='utf-8')
else:
    proof = json.loads(proof_path.read_text(encoding='utf-8'))
    replay = call('/shipments', a, proof['body'], proof['key'], 201)
    assert replay['id'] == proof['id'], 'Idempotency receipt did not survive restart'
detail = call('/shipments/' + proof['id'], a)
assert detail['shipment']['status'] == 'DRAFT'
assert detail['shipment']['createdAt'] == proof['createdAt']
assert len(detail['events']) == 1
call('/shipments/' + proof['id'], b, expected=404)
assert all(row['id'] != proof['id'] for row in call('/shipments', b))
print(json.dumps({'mode': mode, 'status': 'PASS', 'shipmentId': proof['id'], 'reference': proof['reference'], 'events': len(detail['events']), 'database': 'isolated H2 file'}, indent=2))
