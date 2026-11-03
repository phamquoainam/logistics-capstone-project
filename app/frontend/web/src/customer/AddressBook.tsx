import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { customerClient, CustomerApiError } from './client';
import type { AddressInput, AddressOperation, AddressPage, PersonalAddress } from './client';

const blank = (): AddressInput => ({ kind: 'SENDER', contactName: '', contactPhone: '', address: { country: 'VN', administrativeArea: '', locality: '', addressLine: '' }, isDefault: false });
const inputOf = (item: PersonalAddress): AddressInput => ({ kind: item.kind, ...(item.label ? { label: item.label } : {}), contactName: item.contactName, contactPhone: item.contactPhone, address: { ...item.address }, isDefault: item.isDefault });
function errorMessage(error: unknown) {
  const code = error instanceof CustomerApiError ? error.code : '';
  return ({ VALIDATION_FAILED: 'Thông tin chưa hợp lệ. Hãy kiểm tra tên, số điện thoại và địa chỉ.', PRECONDITION_STALE: 'Địa chỉ đã thay đổi ở nơi khác. Tải lại bản hiện tại trước khi lưu.', RESOURCE_NOT_FOUND: 'Địa chỉ không còn khả dụng trong tài khoản này.', IDEMPOTENCY_CONFLICT: 'Yêu cầu này khác nội dung của thao tác trước. Hãy kiểm tra dữ liệu hiện tại.', NETWORK_UNAVAILABLE: 'Không kết nối được máy chủ. Vui lòng thử lại.', OUTCOME_UNKNOWN: 'Chưa xác định kết quả. Thao tác có thể đã được lưu; hãy kiểm tra kết quả trước khi tiếp tục.', RESPONSE_INVALID: 'Không xác thực được dữ liệu địa chỉ trả về.' } as Record<string, string>)[code] ?? 'Không thể hoàn tất yêu cầu. Vui lòng thử lại sau.';
}
type Command = { operation: AddressOperation; key: string; target?: string; etag?: string; input?: AddressInput };

function Editor({ original, busy, uncertain, submit, cancel }: { original?: PersonalAddress; busy: boolean; uncertain: boolean; submit: (input: AddressInput) => void; cancel: () => void }) {
  const [input, setInput] = useState<AddressInput>(() => original ? inputOf(original) : blank());
  const dirty = JSON.stringify(input) !== JSON.stringify(original ? inputOf(original) : blank());
  useEffect(() => { if (!dirty && !uncertain) return; const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [dirty, uncertain]);
  function field<K extends keyof AddressInput>(key: K, value: AddressInput[K]) { setInput(previous => ({ ...previous, [key]: value })); }
  function save(event: FormEvent) { event.preventDefault(); const value = { ...input }; if (!value.label?.trim()) delete value.label; submit(value); }
  return <section className="pw-panel ab-editor" aria-labelledby="address-editor-title"><h2 id="address-editor-title">{original ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'}</h2><p>Thông tin do bạn tự nhập, chưa được xác minh vùng phục vụ.</p>
    <form onSubmit={save}><fieldset disabled={busy || uncertain}><div className="ab-form-grid">
      <div><label htmlFor="address-kind">Loại liên hệ</label><select id="address-kind" value={input.kind} onChange={e => field('kind', e.target.value as AddressInput['kind'])}><option value="SENDER">Người gửi</option><option value="RECIPIENT">Người nhận</option></select></div>
      <div><label htmlFor="address-label">Tên gợi nhớ <span>(không bắt buộc)</span></label><input id="address-label" maxLength={80} placeholder="Ví dụ: Nhà riêng" value={input.label ?? ''} onChange={e => field('label', e.target.value)} /></div>
      <div><label htmlFor="address-name">Họ tên liên hệ</label><input id="address-name" required maxLength={160} autoComplete="name" value={input.contactName} onChange={e => field('contactName', e.target.value)} /></div>
      <div><label htmlFor="address-phone">Số điện thoại liên hệ</label><input id="address-phone" required type="tel" maxLength={32} autoComplete="tel" placeholder="Nhập 0… hoặc +84…" value={input.contactPhone} onChange={e => field('contactPhone', e.target.value)} /></div>
      <div><label htmlFor="address-area">Tỉnh / thành phố</label><input id="address-area" required maxLength={160} autoComplete="address-level1" value={input.address.administrativeArea} onChange={e => field('address', { ...input.address, administrativeArea: e.target.value })} /></div>
      <div><label htmlFor="address-locality">Phường / xã / địa phương</label><input id="address-locality" required maxLength={160} autoComplete="address-level2" value={input.address.locality} onChange={e => field('address', { ...input.address, locality: e.target.value })} /></div>
    </div><label htmlFor="address-line">Địa chỉ chi tiết</label><textarea id="address-line" required maxLength={500} rows={3} autoComplete="street-address" value={input.address.addressLine} onChange={e => field('address', { ...input.address, addressLine: e.target.value })} />
      <label className="ab-checkbox"><input type="checkbox" checked={input.isDefault} onChange={e => field('isDefault', e.target.checked)} />Đặt làm mặc định cho {input.kind === 'SENDER' ? 'người gửi' : 'người nhận'}</label>
      <p className="ab-hint">Việt Nam · Số điện thoại này chỉ là liên hệ, không thay đổi thông tin đăng nhập. Mỗi loại có tối đa một địa chỉ mặc định.</p></fieldset>
      <div className="ab-actions"><button className="pw-primary" disabled={busy || uncertain} type="submit">{busy ? 'Đang lưu…' : 'Lưu địa chỉ'}</button><button className="pw-secondary" disabled={busy || uncertain} type="button" onClick={() => { if (!dirty || window.confirm('Bỏ các thay đổi địa chỉ chưa lưu?')) cancel(); }}>Đóng chỉnh sửa</button></div>
    </form></section>;
}

export default function AddressBook() {
  const [data, setData] = useState<AddressPage>(), [loading, setLoading] = useState(true), [loadError, setLoadError] = useState('');
  const [query, setQuery] = useState({ search: '', kind: '' }), [search, setSearch] = useState(''), [kind, setKind] = useState(''), [page, setPage] = useState(0), [revision, setRevision] = useState(0);
  const [editor, setEditor] = useState<{ original?: PersonalAddress }>(), [archive, setArchive] = useState<PersonalAddress>(), [busy, setBusy] = useState(false), [uncertain, setUncertain] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  const pending = useRef<Command | undefined>(undefined), running = useRef(false), mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; pending.current = undefined; }; }, []);
  useEffect(() => { if (!uncertain) return; const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [uncertain]);
  useEffect(() => {
    let active = true; setLoading(true); setLoadError('');
    customerClient.listAddresses(page, query.search, query.kind).then(value => { if (active) setData(value); }).catch(failure => { if (active) setLoadError(errorMessage(failure)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, query, revision]);
  function completed(operation: AddressOperation) { pending.current = undefined; setUncertain(false); setEditor(undefined); setArchive(undefined); setError(''); setSuccess(operation === 'archivePersonalAddress' ? 'Đã lưu trữ địa chỉ.' : 'Đã lưu địa chỉ.'); setRevision(value => value + 1); }
  async function execute(command: Omit<Command, 'key'>) {
    if (running.current || pending.current) return; running.current = true; setBusy(true); setError(''); setSuccess('');
    const request = { ...command, key: crypto.randomUUID() }; pending.current = request;
    try {
      if (request.operation === 'createPersonalAddress') await customerClient.createAddress(request.input!, request.key);
      else if (request.operation === 'updatePersonalAddress') await customerClient.updateAddress(request.target!, request.input!, request.etag!, request.key);
      else await customerClient.archiveAddress(request.target!, request.etag!, request.key);
      if (mounted.current) completed(request.operation);
    } catch (failure) {
      if (mounted.current) {
        const unknown = failure instanceof CustomerApiError && (failure.code === 'OUTCOME_UNKNOWN' || failure.status >= 500);
        setUncertain(unknown); setError(errorMessage(unknown ? new CustomerApiError('OUTCOME_UNKNOWN') : failure)); if (!unknown) pending.current = undefined;
      }
    } finally { running.current = false; if (mounted.current) setBusy(false); }
  }
  async function resolve() {
    if (running.current || !pending.current) return; running.current = true; setBusy(true); const request = pending.current;
    try {
      const receipt = await customerClient.addressReceipt(request.operation, request.key);
      if (request.target && receipt.targetId !== request.target) throw new CustomerApiError('RESPONSE_INVALID');
      if (mounted.current) completed(request.operation);
    } catch { if (mounted.current) setError('Chưa xác nhận được kết quả. Yêu cầu vẫn chưa xác định; không gửi thao tác mới hoặc tải lại trang.'); }
    finally { running.current = false; if (mounted.current) setBusy(false); }
  }
  return <section className="ab-page"><div className="pw-page-heading"><div><span className="pw-eyebrow">ARGOPOST PERSONAL</span><h1 tabIndex={-1}>Sổ địa chỉ</h1><p>Giữ thông tin người gửi và người nhận trong không gian của riêng bạn.</p></div><button className="pw-primary" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { setEditor({}); setError(''); setSuccess(''); }}>+ Thêm địa chỉ</button></div>
    <p className="ab-disclaimer">Liên hệ tự nhập, chưa xác minh. Vùng phục vụ được kiểm tra riêng khi báo giá.</p>
    {success && <p className="cp-notice" role="status">{success}</p>}{error && <p className="cp-notice cp-error" role="alert">{error}</p>}
    {uncertain && <button className="pw-secondary" disabled={busy} onClick={() => { void resolve(); }}>{busy ? 'Đang kiểm tra…' : 'Kiểm tra kết quả thao tác'}</button>}
    {editor && <Editor key={editor.original ? `${editor.original.id}:${editor.original.version}` : 'new'} original={editor.original} busy={busy} uncertain={uncertain} submit={input => { void execute({ operation: editor.original ? 'updatePersonalAddress' : 'createPersonalAddress', input, ...(editor.original ? { target: editor.original.id, etag: '"v' + editor.original.version + '"' } : {}) }); }} cancel={() => { setEditor(undefined); setError(''); }} />}
    {archive && <section className="pw-panel ab-archive" role="alertdialog" aria-labelledby="archive-address-title" aria-describedby="archive-address-description"><h2 id="archive-address-title">Lưu trữ địa chỉ này?</h2><p id="archive-address-description">Địa chỉ sẽ không còn trong sổ đang hoạt động. Nếu là mặc định, lựa chọn mặc định này được gỡ bỏ. Lịch sử vẫn được bảo toàn.</p><div className="ab-actions"><button className="pw-secondary" disabled={busy || uncertain} onClick={() => setArchive(undefined)}>Giữ địa chỉ</button><button className="pw-primary" disabled={busy || uncertain} onClick={() => { void execute({ operation: 'archivePersonalAddress', target: archive.id, etag: '"v' + archive.version + '"' }); }}>Xác nhận lưu trữ</button></div></section>}
    {!busy && !uncertain && error && (editor?.original || archive) && <button className="pw-secondary" onClick={() => { if (window.confirm('Tải lại bản mới sẽ bỏ nội dung chưa lưu. Tiếp tục?')) { setEditor(undefined); setArchive(undefined); setError(''); setRevision(value => value + 1); } }}>Tải lại bản hiện tại</button>}
    <form className="pw-panel ab-filters" onSubmit={event => { event.preventDefault(); setPage(0); setQuery({ search: search.trim(), kind }); }}><div><label htmlFor="address-search">Tìm liên hệ hoặc địa chỉ</label><input id="address-search" maxLength={160} value={search} onChange={e => setSearch(e.target.value)} placeholder="Tên, số điện thoại, địa chỉ…" /></div><div><label htmlFor="address-filter-kind">Loại địa chỉ</label><select id="address-filter-kind" value={kind} onChange={e => setKind(e.target.value)}><option value="">Tất cả</option><option value="SENDER">Người gửi</option><option value="RECIPIENT">Người nhận</option></select></div><button className="pw-secondary" type="submit">Tìm địa chỉ</button></form>
    {loading ? <p role="status" className="pw-profile-loading">Đang tải sổ địa chỉ…</p> : loadError ? <div className="cp-notice cp-error"><p role="alert">{loadError}</p><button className="pw-secondary" onClick={() => setRevision(value => value + 1)}>Tải lại danh sách</button></div> : data && <>
      {data.items.length === 0 ? <div className="pw-panel ab-empty"><h2>{query.search || query.kind ? 'Không tìm thấy địa chỉ phù hợp' : 'Sổ địa chỉ của bạn đang trống'}</h2><p>{query.search || query.kind ? 'Thử thay đổi nội dung tìm kiếm hoặc bộ lọc.' : 'Thêm liên hệ đầu tiên để lưu thông tin người gửi hoặc người nhận.'}</p></div> : <div className="ab-list">{data.items.map(item => <article className="pw-panel ab-card" key={item.id}><div className="ab-card-heading"><span className="ab-kind">{item.kind === 'SENDER' ? 'Người gửi' : 'Người nhận'}</span>{item.isDefault && <span className="ab-default">✓ Mặc định</span>}<span className="pw-tag">Chưa xác minh</span></div><h2>{item.label ?? item.contactName}</h2>{item.label && <p className="ab-contact-name">{item.contactName}</p>}<p>{item.contactPhone}</p><address>{item.address.addressLine}<br />{item.address.locality}, {item.address.administrativeArea}, Việt Nam</address><div className="ab-actions"><button className="pw-secondary" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { setEditor({ original: item }); setError(''); setSuccess(''); }}>Chỉnh sửa</button><button className="ab-archive-button" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { setArchive(item); setError(''); setSuccess(''); }}>Lưu trữ</button></div></article>)}</div>}
      <nav className="ab-pagination" aria-label="Phân trang sổ địa chỉ"><button className="pw-secondary" disabled={page === 0 || busy || uncertain} onClick={() => setPage(value => value - 1)}>Trang trước</button><span>Trang {page + 1}</span><button className="pw-secondary" disabled={!data.hasNext || busy || uncertain} onClick={() => setPage(value => value + 1)}>Trang sau</button></nav>
    </>}
  </section>;
}
