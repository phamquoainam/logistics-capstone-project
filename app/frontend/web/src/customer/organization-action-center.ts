import { organizationQuery } from './organization-management';

/** Existing Management selectors only; never forward an unapproved action query. */
export function actionSourceQuery(raw: URLSearchParams) {
  const parsed = organizationQuery(raw), filters = new URLSearchParams(parsed.api);
  filters.delete('page');
  const conflictingAssignee = filters.has('assignee') && filters.get('assignee') !== 'NONE';
  return { valid: parsed.valid && parsed.category === 'ACTION', filters,
    draftComparable: filters.size === 0, conflictingAssignee };
}
export function unassignedActionQuery(filters: URLSearchParams, page: number) {
  if (!Number.isSafeInteger(page) || page < 0 || page > 2147483647 || filters.has('assignee') && filters.get('assignee') !== 'NONE') throw new Error('ACTION_SOURCE_FILTER_CONFLICT');
  const query = new URLSearchParams(filters); query.set('assignee', 'NONE'); query.set('page', String(page)); return query;
}
