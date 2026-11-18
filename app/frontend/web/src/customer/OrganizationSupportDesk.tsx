import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { Subject } from './client';
import type { OrganizationTicketDetail, OrganizationTicketPage } from './organization-support-data';
import { organizationTicketStates } from './organization-support-data';
import { shipmentDate } from './shipment-management';
import './organization-support.css';
const uuid = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/;
const labels: Record<string, string> = { START: 'Bắt đầu xử lý', REPLY: 'Gửi phản hồi CSKH', REQUEST_INFORMATION: 'Yêu cầu bổ sung', RESOLVE: 'Gửi kết quả giải quyết', REOPEN: 'Chấp nhận xem xét lại' };
export function OrganizationSupportDesk({ subject }: { subject: Subject }) {
  const [params, setParams] = useSearchParams(), org = params.get('organization') ?? '', target = params.get('case') ?? '';
  const valid = uuid.test(org) && org !== '00000000-0000-0000-0000-000000000000' && (!target || uuid.test(target));
  const [organization, setOrganization] = useState(org), [queue, setQueue] = useState<OrganizationTicketPage>(), [detail, setDetail] = useState<OrganizationTicketDetail>(), [error, setError] = useState(''), [message, setMessage] = useState(''), [revision, setRevision] = useState(0), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false);
  const pending = useRef<{ key: string; op: 'CARRIER_ASSIGN' | 'CARRIER_REPLY' | 'CARRIER_TRANSITION'; target: string } | undefined>(undefined), running = useRef(false);
  const page = /^(0|[1-9][0-9]{0,5})$/.test(params.get('page') ?? '0') ? Number(params.get('page') ?? 0) : 0;
  const loaded = JSON.stringify([org, target, page, revision]), [loadedKey, setLoadedKey] = useState('');
  useEffect(() => {
    if (!busy && !uncertain) return;
    const unload = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ''; };
    const navigate = (event: MouseEvent) => { if ((event.target as Element).closest('a[href]') && !window.confirm('Kết quả gửi đang chờ kiểm tra. Rời trang có thể làm mất mã kiểm tra thao tác. Bạn vẫn muốn rời trang?')) { event.preventDefault(); event.stopPropagation(); } };
    window.addEventListener('beforeunload', unload); document.addEventListener('click', navigate, true);
    return () => { window.removeEventListener('beforeunload', unload); document.removeEventListener('click', navigate, true); };
  }, [busy, uncertain]);
  useEffect(() => { if (!valid) return; let live = true; const read = target ? customerClient.organizationSupportCase(org, target).then(v => { if (live) setDetail(v); }) : customerClient.organizationSupportQueue(org, page).then(v => { if (live) setQueue(v); });
    void read.then(() => { if (live) { setError(''); setLoadedKey(loaded); } }).catch(() => { if (live) { setDetail(undefined); setQueue(undefined); setError('Không có thẩm quyền hoặc không tải được hồ sơ doanh nghiệp trong phạm vi được cấp.'); setLoadedKey(loaded); } }); return () => { live = false; };
  }, [valid, org, target, page, revision, loaded]);
  async function command(id: string, version: number, action: string) {
    if (running.current || uncertain) return; running.current = true; setBusy(true); setError(''); const op = action === 'ASSIGN' ? 'CARRIER_ASSIGN' : action === 'REPLY' ? 'CARRIER_REPLY' : 'CARRIER_TRANSITION'; const key = crypto.randomUUID(); pending.current = { key, op, target: id };
    try { if (action === 'ASSIGN') await customerClient.assignOrganizationSupportCase(org, id, subject.accountId, version, key); else await customerClient.commandOrganizationSupportCase(org, id, action, message, version, key); pending.current = undefined; setMessage(''); setParams({ organization: org, case: id }); setRevision(v => v + 1); }
    catch (e) { if (e instanceof CustomerApiError && e.code === 'OUTCOME_UNKNOWN') { setUncertain(true); setError('Kết quả chưa xác định. Kiểm tra biên nhận trước khi gửi lệnh khác.'); } else { pending.current = undefined; setError('Lệnh bị từ chối hoặc hồ sơ đã thay đổi. Tải lại để kiểm tra thẩm quyền và trạng thái.'); } } finally { running.current = false; setBusy(false); }
  }
  async function recover() { const p = pending.current; if (!p || running.current) return; running.current = true; setBusy(true); try { const r = await customerClient.organizationCarrierSupportReceipt(p.op, p.key); if (r.targetId !== p.target) throw new Error(); pending.current = undefined; setUncertain(false); setError(''); setParams({ organization: org, case: p.target }); setRevision(v => v + 1); } catch { setError('Chưa xác nhận được kết quả. Chưa gửi lại lệnh.'); } finally { running.current = false; setBusy(false); } }
  return <section className="os-support su-workbench"><header className="bw-heading"><div><span className="pw-eyebrow">CSKH ARGOPOST</span><h1>Hỗ trợ doanh nghiệp</h1><p>Canonical Staff grant, Organization binding và phân công hồ sơ được kiểm tra tại Backend.</p></div><Link to="/operations/support">Hỗ trợ cá nhân</Link></header>
    <section className="bw-panel os-card"><form className="os-ticket-filters" onSubmit={e => { e.preventDefault(); if (!busy && !uncertain) setParams({ organization }); }}><label>Organization được phân công<input required value={organization} disabled={busy || uncertain} onChange={e => setOrganization(e.target.value)} /></label><button className="pw-secondary" disabled={busy || uncertain}>Mở hàng đợi doanh nghiệp</button></form>
      {error && <p role="alert">{error}</p>}{uncertain && <button className="pw-secondary" disabled={busy} onClick={() => { void recover(); }}>Kiểm tra biên nhận CSKH</button>}
      {!valid ? <p>Nhập đúng Organization ID được operator cấp binding. Không có quyền duyệt danh sách doanh nghiệp toàn nền tảng.</p> : loadedKey !== loaded ? <p role="status">Đang tải hồ sơ…</p> : !error && (target ? detail && <><h2>{detail.subject}</h2><p>{organizationTicketStates[detail.state]} · {detail.code}</p><ol className="os-conversation" aria-label="Hội thoại CSKH doanh nghiệp">{detail.publicMessages.map(m => <li key={m.id} className={m.authorKind === 'SUPPORT' ? 'os-carrier-reply' : ''}><header><strong>{m.authorKind === 'SUPPORT' ? 'CSKH ArgoPost' : 'Doanh nghiệp'}</strong><time>{shipmentDate(m.createdAt)}</time></header><p>{m.message}</p></li>)}</ol><label>Nội dung công khai cho doanh nghiệp<textarea rows={4} maxLength={4000} disabled={busy || uncertain} value={message} onChange={e => setMessage(e.target.value)} /></label><div className="os-context-actions">{detail.allowedActions.map(action => <button className="pw-secondary" key={action} disabled={busy || uncertain || !message.trim()} onClick={() => { void command(detail.id, detail.version, action); }}>{labels[action]}</button>)}</div></> : queue && <><h2>Hàng đợi Ticket doanh nghiệp</h2>{!queue.items.length ? <p>Chưa có Ticket mới trong Organization này.</p> : <ul className="su-ticket-list">{queue.items.map(t => <li key={t.id}><div><strong>{t.subject}</strong><small className="os-ticket-code">{t.code}</small></div><button className="pw-secondary" disabled={busy || uncertain} onClick={() => { void command(t.id, t.version, 'ASSIGN'); }}>Nhận xử lý doanh nghiệp</button></li>)}</ul>}<div className="os-context-actions"><button className="pw-secondary" disabled={busy || uncertain || !page} onClick={() => setParams({ organization: org, page: String(page - 1) })}>Trang trước</button><button className="pw-secondary" disabled={busy || uncertain || !queue.hasNext} onClick={() => setParams({ organization: org, page: String(page + 1) })}>Trang tiếp</button></div></>)}
      {valid && <div className="os-context-actions"><button className="pw-secondary" disabled={busy || uncertain} onClick={() => setRevision(v => v + 1)}>Cập nhật hồ sơ doanh nghiệp</button>{target && <button className="pw-secondary" disabled={busy || uncertain} onClick={() => setParams({ organization: org })}>Về hàng đợi doanh nghiệp</button>}</div>}
    </section>
  </section>;
}
