import type { SupportDetail, SupportSummary } from '../../src/customer/support-ticket-data';
export const supportId = '10000000-0000-4000-8000-000000000077';
export const supportSummaryFixture: SupportSummary = { id: supportId, code: 'AP-T-' + supportId.replaceAll('-', ''), kind: 'GENERAL', shipmentId: null, category: 'ACCOUNT_HELP', subject: 'Synthetic support question', state: 'NEW', version: 1, createdAt: '2026-11-01T01:00:00Z', updatedAt: '2026-11-01T01:00:00Z', priority: 'NORMAL', responseDueAt: null, teamLabel: null };
/** Exact accepted flat wire; nested ticket exists only in the parsed UI model. */
export function supportDetailFixture(summary: SupportSummary = supportSummaryFixture, messages?: SupportDetail['publicMessages'], actions = ['REPLY']) {
  return { ...summary, publicMessages: messages ?? [{ id: '10000000-0000-4000-8000-000000000078', authorKind: 'CUSTOMER', message: 'Synthetic initial message', createdAt: summary.createdAt, eventType: 'CREATED' }], allowedActions: actions };
}
