import { AdminSessionError } from './session';
import { governanceId } from './governance';
export type AuditDomain = 'MERCHANT_APPLICATION' | 'MERCHANT_ORGANIZATION';
export type AuditRow = { id: string; actorId: string; operation: string; targetType: 'APPLICATION' | 'ORGANIZATION'; targetId: string; priorVersion: number; newVersion: number; revision: number | null; policyVersion: string; reasonCode: string; occurredAt: string; requestId: string };
export type AuditPage = { items: AuditRow[]; page: number; size: number; hasNext: boolean };
const bad = (): never => { throw new AdminSessionError('RESPONSE_INVALID'); };
const shape = (v: unknown, keys: string[]) => { if (!v || typeof v !== 'object' || Array.isArray(v) || Object.keys(v).length !== keys.length || keys.some(k => !Object.hasOwn(v, k))) bad();return v as Record<string, unknown>; };
export function parseGovernanceAudit(value: unknown, domain: AuditDomain, page: number, target?: string): AuditPage {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']);
  if (v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const ids = new Set<string>(), type = domain === 'MERCHANT_APPLICATION' ? 'APPLICATION' : 'ORGANIZATION';
  const operations = type === 'APPLICATION' ? ['SUBMIT', 'RESUBMIT', 'WITHDRAW', 'ASSIGN_REVIEWER', 'BEGIN_REVIEW', 'READY_FOR_DECISION', 'REQUEST_INFORMATION', 'APPROVE', 'REJECT', 'READ_DISCLOSURE', 'RESERVE_EVIDENCE', 'FINALIZE_EVIDENCE', 'ARCHIVE_EVIDENCE'] : ['SUSPEND', 'REINSTATE'];
  for (const row of v.items as unknown[]) {
    const e = shape(row, ['id', 'actorId', 'operation', 'targetType', 'targetId', 'priorVersion', 'newVersion', 'revision', 'policyVersion', 'reasonCode', 'occurredAt', 'requestId']);
    if (!governanceId(e.id) || ids.has(e.id) || !governanceId(e.actorId) || !governanceId(e.requestId) || !governanceId(e.targetId) || target && e.targetId !== target || e.targetType !== type || !operations.includes(String(e.operation)) || !Number.isSafeInteger(e.priorVersion) || Number(e.priorVersion) < 0 || !Number.isSafeInteger(e.newVersion) || Number(e.newVersion) < Number(e.priorVersion) || e.revision !== null && (!Number.isSafeInteger(e.revision) || Number(e.revision) < 1) || typeof e.policyVersion !== 'string' || !e.policyVersion.trim() || e.policyVersion.length > 64 || typeof e.reasonCode !== 'string' || !/^[A-Z_]{1,40}$/.test(e.reasonCode) || typeof e.occurredAt !== 'string' || !e.occurredAt.endsWith('Z') || !Number.isFinite(Date.parse(e.occurredAt))) bad();
    ids.add(e.id as string);
  }
  return v as AuditPage;
}
