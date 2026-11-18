import { useEffect, useState } from 'react';
import type { BusinessContext } from './business-data';
import { customerClient } from './client';
import type { TeamAuditOperation, TeamAuditPage, TeamAuditQuery } from './organization-team-data';
import { teamAuditOperations } from './organization-team-data';
const operations: Record<TeamAuditOperation, string> = { INVITE: 'Mời thành viên', RESEND: 'Gửi lại lời mời', REVOKE_INVITATION: 'Thu hồi lời mời', ACCESS: 'Cập nhật quyền', SUSPEND: 'Tạm khóa', RESUME: 'Mở lại', REVOKE_MEMBER: 'Thu hồi Membership', ACCEPT: 'Chấp nhận lời mời', DECLINE: 'Từ chối lời mời' };
const day = 86400000;
const localDay = (time: number) => new Date(time + 7 * 3600000).toISOString().slice(0, 10);
function range(from: string, to: string, operation: TeamAuditOperation | null): TeamAuditQuery {
  return { membershipId: null, operation, from: new Date(from + 'T00:00:00+07:00').toISOString(), to: new Date(Date.parse(to + 'T00:00:00+07:00') + day).toISOString(), page: 0, size: 20 };
}
export function OrganizationTeamAudit({ context }: { context: BusinessContext }) {
  const [from, setFrom] = useState(() => localDay(Date.now() - 6 * day)), [to, setTo] = useState(() => localDay(Date.now())), [operation, setOperation] = useState<TeamAuditOperation | null>(null);
  const [query, setQuery] = useState(() => range(from, to, null)), [revision, setRevision] = useState(0), [error, setError] = useState('');
  const [page, setPage] = useState<{ key: string; data?: TeamAuditPage; error?: boolean }>();
  const key = `${context.organizationId}:${context.membershipId}:${context.authorityVersion}:${JSON.stringify(query)}:${revision}`;
  useEffect(() => { let current = true; void customerClient.readTeamAudit(context, query).then(data => { if (current) setPage({ key, data }); }).catch(() => { if (current) setPage({ key, error: true }); }); return () => { current = false; }; }, [context, query, key]);
  const current = page?.key === key ? page : undefined;
  return <section className="bw-panel otm-card" aria-label="Lịch sử quản trị nhân sự"><h2>Lịch sử quản trị</h2><p>Thay đổi và quyết định đã ghi nhận; giờ Việt Nam. Tối đa 90 ngày mỗi lần lọc.</p><form className="otm-directory-filters" onSubmit={event => { event.preventDefault(); try { const next = range(from, to, operation); if (Date.parse(next.to) <= Date.parse(next.from) || Date.parse(next.to) - Date.parse(next.from) > 90 * day) throw new Error(); setQuery(next); setRevision(v => v + 1); setError(''); } catch { setError('Chọn khoảng ngày hợp lệ, không quá 90 ngày.'); } }}><label>Từ ngày<input type="date" required value={from} max={to} onChange={event => setFrom(event.target.value)} /></label><label>Đến ngày<input type="date" required value={to} min={from} onChange={event => setTo(event.target.value)} /></label><label>Hoạt động quản trị<select value={operation ?? ''} onChange={event => setOperation((event.target.value || null) as typeof operation)}><option value="">Tất cả hoạt động</option>{teamAuditOperations.map(op => <option key={op} value={op}>{operations[op]}</option>)}</select></label><button className="pw-secondary" type="submit">Lọc lịch sử</button></form>{error && <p role="alert">{error}</p>}
    {!current ? <p role="status">Đang kiểm tra quyền và tải lịch sử…</p> : current.error ? <p role="alert">Chưa tải được lịch sử hoặc quyền xem đã thay đổi. Dữ liệu cũ đã ẩn.</p> : current.data && <><div className="otm-table-wrap"><table><thead><tr><th>Thời gian</th><th>Hoạt động</th><th>Thành viên thực hiện</th><th>Lý do</th></tr></thead><tbody>{current.data.items.map(row => <tr key={row.id}><td>{new Date(row.occurredAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}</td><td>{operations[row.operation]}</td><td>{row.actorMembershipId ? row.actorMembershipId : 'Người nhận lời mời đã xác minh'}</td><td>{row.reason}</td></tr>)}</tbody></table></div>{!current.data.items.length && <p>Chưa có hoạt động khớp bộ lọc.</p>}<nav className="otm-pagination" aria-label="Phân trang lịch sử Team"><span>Trang {query.page + 1}</span><button type="button" className="pw-secondary" disabled={!query.page} onClick={() => setQuery({ ...query, page: query.page - 1 })}>Trước</button><button type="button" className="pw-secondary" disabled={!current.data.hasNext} onClick={() => setQuery({ ...query, page: query.page + 1 })}>Tiếp</button></nav></>}
  </section>;
}
