import { useEffect, useState } from 'react';
import { adminSession, AdminSessionError } from './session';
import { date, daysBefore, todayVietnam } from './data';
import { governanceId } from './governance';
import { parseGovernanceAudit } from './audit-data';
import type { AuditDomain, AuditPage } from './audit-data';
const stamp = (value: string) => new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value));
export default function MerchantAudit() {
  const today = todayVietnam();
  const [from, setFrom] = useState(() => daysBefore(today, 29)), [to, setTo] = useState(today), [domain, setDomain] = useState<AuditDomain>('MERCHANT_APPLICATION'), [target, setTarget] = useState('');
  const [query, setQuery] = useState(() => ({ from: daysBefore(today, 29), to: today, domain: 'MERCHANT_APPLICATION' as AuditDomain, target: '' })), [page, setPage] = useState(0), [refresh, setRefresh] = useState(0);
  const [rows, setRows] = useState<AuditPage | null>(null), [error, setError] = useState(''), [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;setLoading(true);setRows(null);setError('');
    const start = new Date(query.from + 'T00:00:00+07:00'), end = new Date(query.to + 'T00:00:00+07:00');end.setUTCDate(end.getUTCDate() + 1);
    const parameters = new URLSearchParams({ domain: query.domain, from: start.toISOString(), to: end.toISOString(), page: String(page), size: '20' });if (query.target) parameters.set('targetId', query.target);
    void adminSession.read('audit', parameters, value => parseGovernanceAudit(value, query.domain, page, query.target || undefined)).then(value => { if (active) setRows(value); }).catch(failure => { if (active) { const code = failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE';setError(code === 'ACCESS_DENIED' || code === 'RESOURCE_NOT_FOUND' ? 'Phạm vi kiểm toán không còn khả dụng cho tài khoản này.' : 'Chưa tải được lịch sử. Vui lòng thử lại.'); } }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [query, page, refresh]);
  function apply() {
    if (!date(from) || !date(to) || from > to || to > today || (Date.parse(to) - Date.parse(from)) / 86400000 >= 366 || target && !governanceId(target)) { setError('Kiểm tra ngày và UUID đối tượng.');return; }
    setPage(0);setQuery({ from, to, domain, target });
  }
  return <section className="ap-section" aria-labelledby="audit-title">
    <header><h1 id="audit-title">Lịch sử kiểm toán Merchant</h1><p>Sự kiện đã lưu, theo phạm vi được cấp. Giờ Việt Nam.</p></header>
    <form className="ap-filters" onSubmit={event => { event.preventDefault();apply(); }}>
      <label>Đối tượng<select value={domain} onChange={event => setDomain(event.target.value as AuditDomain)}><option value="MERCHANT_APPLICATION">Hồ sơ Merchant</option><option value="MERCHANT_ORGANIZATION">Tổ chức Merchant</option></select></label>
      <label>Từ ngày<input type="date" value={from} max={today} onChange={event => setFrom(event.target.value)} required /></label><label>Đến ngày<input type="date" value={to} max={today} onChange={event => setTo(event.target.value)} required /></label>
      <label>UUID đối tượng (tùy chọn)<input value={target} maxLength={36} spellCheck={false} onChange={event => setTarget(event.target.value)} /></label>
      <button className="ap-primary" type="submit" disabled={loading}>Áp dụng</button><button className="ap-secondary" type="button" onClick={() => setRefresh(n => n + 1)} disabled={loading}>Làm mới</button>
    </form>
    {error && <p role="alert" className="ap-error">{error}</p>}{loading && <p role="status">Đang tải lịch sử…</p>}
    {rows && <><div className="ap-table-scroll"><table><thead><tr><th>Thời gian</th><th>Thao tác</th><th>Đối tượng</th><th>Phiên bản</th><th>Mã lý do</th><th>Mã tương quan</th></tr></thead><tbody>{rows.items.map(row => <tr key={row.id}><td>{stamp(row.occurredAt)}</td><td>{row.operation}</td><td><code>{row.targetId}</code></td><td>{row.priorVersion} / {row.newVersion}</td><td>{row.reasonCode}</td><td><code>{row.requestId}</code></td></tr>)}</tbody></table></div>{rows.items.length === 0 && <p>Không có sự kiện trong bộ lọc này.</p>}
      <nav aria-label="Phân trang lịch sử"><button className="ap-secondary" disabled={loading || page === 0} onClick={() => setPage(n => n - 1)}>Trước</button><span>Trang {page + 1}</span><button className="ap-secondary" disabled={loading || !rows.hasNext} onClick={() => setPage(n => n + 1)}>Tiếp</button></nav></>}
  </section>;
}
