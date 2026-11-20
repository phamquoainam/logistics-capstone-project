import { expect, test } from '@playwright/test';
import { claimEvidence, claimEvidenceList, evidenceInput, evidenceTransfer } from '../src/customer/claim-evidence';

const id = '10000000-0000-4000-8000-000000000001', intake = '20000000-0000-4000-8000-000000000001', org = '30000000-0000-4000-8000-000000000001';
const path = '/api/v1/merchant-organizations/' + org + '/claim-intakes/' + intake + '/evidence/' + id;
async function fixture() { const file = new File([new Uint8Array([1, 2, 3])], 'synthetic.png', { type: 'image/png' }), input = await evidenceInput(file); return { file, metadata: claimEvidence({ id, intakeId: intake, ...input, state: 'CLEAN', version: 3, createdAt: '2026-11-20T00:00:00Z', attachedAt: '2026-11-20T00:00:01Z' }, intake) }; }
test('private evidence rejects object-key injection, unsupported files and unbounded metadata', async () => {
  const { metadata } = await fixture(); expect(claimEvidenceList([metadata], intake)).toHaveLength(1);
  for (const bad of [{ ...metadata, objectKey: 'private/forged' }, { ...metadata, bytes: 5242881 }, { ...metadata, mime: 'text/html' }, { ...metadata, state: 'RESERVED' }, { ...metadata, intakeId: org }]) expect(() => claimEvidence(bad, intake)).toThrow();
  await expect(evidenceInput(new File(['<svg/>'], 'synthetic.svg', { type: 'image/svg+xml' }))).rejects.toThrow('EVIDENCE_CONTENT_REJECTED');
});
test('private download requires safe headers, byte count and exact persisted hash', async () => {
  const { file, metadata } = await fixture(); let calls = 0;
  const transport: typeof fetch = async (_url, init) => { calls++; expect(init?.credentials).toBe('same-origin'); expect(init?.redirect).toBe('error'); expect(init?.referrerPolicy).toBe('no-referrer'); expect(init?.cache).toBe('no-store'); return new Response(file, { headers: { 'Content-Type': metadata.mime, 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Content-Disposition': 'attachment; filename="argopost-evidence-' + id + '.png"' } }); };
  expect(await evidenceTransfer(transport, path, 'synthetic-memory-token', metadata)).toBeInstanceOf(Blob); expect(calls).toBe(1);
  await expect(evidenceTransfer(transport, '/api/v1/public-media/' + id, 'synthetic', metadata)).rejects.toThrow('VALIDATION_FAILED'); expect(calls).toBe(1);
  await expect(evidenceTransfer(transport, path, 'synthetic', { ...metadata, sha256: '0'.repeat(64) })).rejects.toThrow('RESPONSE_INVALID');
});
test('upload network loss retains one resource-bound request and never retries automatically', async () => {
  const { file, metadata } = await fixture(); let calls = 0; const transport: typeof fetch = async () => { calls++; throw new Error('Synthetic transport failure'); };
  await expect(evidenceTransfer(transport, path, 'synthetic', metadata, file, 'synthetic-csrf')).rejects.toThrow('OUTCOME_UNKNOWN'); expect(calls).toBe(1);
  await expect(evidenceTransfer(transport, path, 'synthetic', metadata, new File([new Uint8Array([9])], 'changed.png', { type: 'image/png' }), 'synthetic-csrf')).rejects.toThrow('EVIDENCE_CONTENT_REJECTED'); expect(calls).toBe(1);
});
