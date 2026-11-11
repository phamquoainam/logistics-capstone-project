import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { customerClient, CustomerApiError } from './client';
import type { AddressInput, AddressOperation, AddressPage, PersonalAddress } from './client';
import { addressMatchKey, normalizeAddressInput } from './address-book-data';

const blank = (): AddressInput => ({ kind: 'SENDER', contactName: '', contactPhone: '', address: { country: 'VN', administrativeArea: '', locality: '', addressLine: '' }, isDefault: false });
const inputOf = (item: PersonalAddress): AddressInput => ({ kind: item.kind, ...(item.label ? { label: item.label } : {}), contactName: item.contactName, contactPhone: item.contactPhone, address: { ...item.address }, isDefault: item.isDefault });
function errorMessage(error: unknown) {
  const code = error instanceof CustomerApiError ? error.code : '';
  return ({ VALIDATION_FAILED: 'Thông tin chưa hợp lệ. Hãy kiểm tra tên, số điện thoại và địa chỉ.', PRECONDITION_STALE: 'Địa chỉ đã thay đổi ở nơi khác. Tải lại bản hiện tại trước khi lưu.', RESOURCE_NOT_FOUND: 'Địa chỉ không còn khả dụng trong tài khoản này.', IDEMPOTENCY_CONFLICT: 'Yêu cầu này khác nội dung của thao tác trước. Hãy kiểm tra dữ liệu hiện tại.', NETWORK_UNAVAILABLE: 'Không kết nối được máy chủ. Vui lòng thử lại.', OUTCOME_UNKNOWN: 'Chưa xác định kết quả. Thao tác có thể đã được lưu; hãy kiểm tra kết quả trước khi tiếp tục.', RESPONSE_INVALID: 'Không xác thực được dữ liệu địa chỉ trả về.' } as Record<string, string>)[code] ?? 'Không thể hoàn tất yêu cầu. Vui lòng thử lại sau.';
}
type Command = { operation: AddressOperation; key: string; target?: string; etag?: string; input?: AddressInput };

function Editor({ original, busy, uncertain, submit, cancel }: { original?: PersonalAddress; busy: boolean; uncertain: boolean; submit: (input: AddressInput) => void; cancel: () => void }) {
  const [input, setInput] = useState<AddressInput>(() => original ? inputOf(original) : blank());
  const form = useRef<HTMLFormElement>(null), scan = useRef(0);
  const [checking, setChecking] = useState(false), [check, setCheck] = useState<{ key: string; message: string }>();
  useEffect(() => () => { scan.current++; }, []);
  const dirty = JSON.stringify(input) !== JSON.stringify(original ? inputOf(original) : blank());
  useEffect(() => { if (!dirty && !uncertain) return; const warn = (event: BeforeUnloadEvent) => { event.preventDefault(); }; window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn); }, [dirty, uncertain]);
  function field<K extends keyof AddressInput>(key: K, value: AddressInput[K]) { setInput(previous => ({ ...previous, [key]: value })); }
  function save(event: FormEvent) { event.preventDefault(); const value = { ...input }; if (!value.label?.trim()) delete value.label; submit(value); }
  async function checkDuplicates() {
    if (checking || busy || uncertain || !form.current?.reportValidity()) return;
    const token = ++scan.current, key = addressMatchKey(input), value = normalizeAddressInput(input);
    setChecking(true); setCheck(undefined);
    try {
      const seen = new Set<string>();
      for (let page = 0; page < 100; page++) {
        const data = await customerClient.listAddresses(page, value.contactPhone, value.kind);
        if (token !== scan.current) return;
        for (const item of data.items) {
          if (seen.has(item.id)) throw new Error('ADDRESS_SCAN_CHANGED'); seen.add(item.id);
          if (item.id !== original?.id && addressMatchKey(item) === key) { setCheck({ key, message: 'Có liên hệ đang hoạt động trùng tên, điện thoại và địa chỉ. Hãy xem lại trước khi lưu; hệ thống không tự gộp liên hệ.' }); return; }
        }
        if (!data.hasNext) { setCheck({ key, message: 'Không thấy liên hệ trùng tại lần kiểm tra này. Đây là gợi ý, không phải xác minh địa chỉ.' }); return; }
      }
      throw new Error('ADDRESS_SCAN_BOUNDED');
    } catch { if (token === scan.current) setCheck({ key, message: 'Chưa kiểm tra được toàn bộ liên hệ. Không thể kết luận không trùng; bạn có thể thử lại.' }); }
    finally { if (token === scan.current) setChecking(false); }
  }
  return <section className="pw-panel ab-editor" aria-labelledby="address-editor-title"><h2 id="address-editor-title">{original ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'}</h2><p>Thông tin do bạn tự nhập, chưa được xác minh vùng phục vụ.</p>
    <form ref={form} onSubmit={save}><fieldset disabled={busy || uncertain || checking}><div className="ab-form-grid">
      <div><label htmlFor="address-kind">Loại liên hệ</label><select id="address-kind" value={input.kind} onChange={e => field('kind', e.target.value as AddressInput['kind'])}><option value="SENDER">Người gửi</option><option value="RECIPIENT">Người nhận</option></select></div>
      <div><label htmlFor="address-label">Tên gợi nhớ <span>(không bắt buộc)</span></label><input id="address-label" maxLength={80} placeholder="Ví dụ: Nhà riêng" value={input.label ?? ''} onChange={e => field('label', e.target.value)} /></div>
      <div><label htmlFor="address-name">Họ tên liên hệ</label><input id="address-name" required maxLength={160} autoComplete="name" value={input.contactName} onChange={e => field('contactName', e.target.value)} /></div>
      <div><label htmlFor="address-phone">Số điện thoại liên hệ</label><input id="address-phone" required type="tel" maxLength={32} autoComplete="tel" placeholder="Nhập 0… hoặc +84…" value={input.contactPhone} onChange={e => field('contactPhone', e.target.value)} /></div>
      <div><label htmlFor="address-area">Tỉnh / thành phố</label><input id="address-area" required maxLength={160} autoComplete="address-level1" value={input.address.administrativeArea} onChange={e => field('address', { ...input.address, administrativeArea: e.target.value })} /></div>
      <div><label htmlFor="address-locality">Phường / xã / địa phương</label><input id="address-locality" required maxLength={160} autoComplete="address-level2" value={input.address.locality} onChange={e => field('address', { ...input.address, locality: e.target.value })} /></div>
    </div><label htmlFor="address-line">Địa chỉ chi tiết</label><textarea id="address-line" required maxLength={500} rows={3} autoComplete="street-address" value={input.address.addressLine} onChange={e => field('address', { ...input.address, addressLine: e.target.value })} />
      <label className="ab-checkbox"><input type="checkbox" checked={input.isDefault} onChange={e => field('isDefault', e.target.checked)} />Đặt làm mặc định cho {input.kind === 'SENDER' ? 'người gửi' : 'người nhận'}</label>
      <p className="ab-hint">Việt Nam · Số điện thoại này chỉ là liên hệ, không thay đổi thông tin đăng nhập. Mỗi loại có tối đa một địa chỉ mặc định.</p></fieldset>
      <div className="ab-tools"><button className="sd-link" type="button" disabled={busy || uncertain || checking} onClick={() => { setInput(normalizeAddressInput(input)); setCheck(undefined); }}>Chuẩn hóa cách nhập</button><button className="sd-link" type="button" disabled={busy || uncertain || checking} onClick={() => { void checkDuplicates(); }}>{checking ? 'Đang kiểm tra…' : 'Kiểm tra trùng liên hệ'}</button></div>
      <p className="ab-hint">Chuẩn hóa khoảng trắng và số điện thoại; không tự sửa tên đường hoặc địa giới.</p>{check?.key === addressMatchKey(input) && <p className="ab-check-result" role="status">{check.message}</p>}
      <div className="ab-actions"><button className="pw-primary" disabled={busy || uncertain || checking} type="submit">{busy ? 'Đang lưu…' : 'Lưu địa chỉ'}</button><button className="pw-secondary" disabled={busy || uncertain || checking} type="button" onClick={() => { if (!dirty || window.confirm('Bỏ các thay đổi địa chỉ chưa lưu?')) cancel(); }}>Đóng chỉnh sửa</button></div>
    </form></section>;
}

export default function AddressBook() {
  const navigate = useNavigate();
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
    {archive && <section className="pw-panel ab-archive" role="alertdialog" aria-labelledby="archive-address-title" aria-describedby="archive-address-description"><h2 id="archive-address-title">Lưu trữ địa chỉ này?</h2><p id="archive-address-description">Địa chỉ sẽ không còn trong sổ đang hoạt động. Nếu là mặc định, lựa chọn mặc định này được gỡ bỏ. Lịch sử vẫn được bảo toàn. Chính sách hiện tại chưa hỗ trợ khôi phục hoặc chỉnh sửa liên hệ đã lưu trữ. Thông tin trên vận đơn đã tạo không thay đổi.</p><div className="ab-actions"><button className="pw-secondary" disabled={busy || uncertain} onClick={() => setArchive(undefined)}>Giữ địa chỉ</button><button className="pw-primary" disabled={busy || uncertain} onClick={() => { void execute({ operation: 'archivePersonalAddress', target: archive.id, etag: '"v' + archive.version + '"' }); }}>Xác nhận lưu trữ</button></div></section>}
    {!busy && !uncertain && error && (editor?.original || archive) && <button className="pw-secondary" onClick={() => { if (window.confirm('Tải lại bản mới sẽ bỏ nội dung chưa lưu. Tiếp tục?')) { setEditor(undefined); setArchive(undefined); setError(''); setRevision(value => value + 1); } }}>Tải lại bản hiện tại</button>}
    {!busy && !uncertain && error && !editor && !archive && <button className="pw-secondary" onClick={() => { setError(''); setRevision(v => v + 1); }}>Tải lại danh sách địa chỉ</button>}
    <form className="pw-panel ab-filters" onSubmit={event => { event.preventDefault(); setPage(0); setQuery({ search: search.trim(), kind }); }}><div><label htmlFor="address-search">Tìm liên hệ hoặc địa chỉ</label><input id="address-search" maxLength={160} value={search} onChange={e => setSearch(e.target.value)} placeholder="Tên, số điện thoại, địa chỉ…" /></div><div><label htmlFor="address-filter-kind">Loại địa chỉ</label><select id="address-filter-kind" value={kind} onChange={e => setKind(e.target.value)}><option value="">Tất cả</option><option value="SENDER">Người gửi</option><option value="RECIPIENT">Người nhận</option></select></div><button className="pw-secondary" type="submit">Tìm địa chỉ</button><button className="sd-link" type="button" onClick={() => { setSearch(''); setKind(''); setPage(0); setQuery({ search: '', kind: '' }); }}>Đặt lại bộ lọc</button></form>
    {loading ? <p role="status" className="pw-profile-loading">Đang tải sổ địa chỉ…</p> : loadError ? <div className="cp-notice cp-error"><p role="alert">{loadError}</p><button className="pw-secondary" onClick={() => setRevision(value => value + 1)}>Tải lại danh sách</button></div> : data && <>
      {data.items.length === 0 ? <div className="pw-panel ab-empty"><h2>{page > 0 ? 'Trang này không còn liên hệ' : query.search || query.kind ? 'Không tìm thấy địa chỉ phù hợp' : 'Sổ địa chỉ của bạn đang trống'}</h2><p>{page > 0 ? 'Giữ bộ lọc và quay lại trang đầu để xem dữ liệu hiện tại.' : query.search || query.kind ? 'Thử thay đổi nội dung tìm kiếm hoặc bộ lọc.' : 'Thêm liên hệ đầu tiên để lưu thông tin người gửi hoặc người nhận.'}</p>{page > 0 && <button className="pw-secondary" onClick={() => setPage(0)}>Về trang đầu</button>}</div> : <div className="ab-list">{data.items.map(item => <article className="pw-panel ab-card" key={item.id}><div className="ab-card-heading"><span className="ab-kind">{item.kind === 'SENDER' ? 'Người gửi' : 'Người nhận'}</span>{item.isDefault && <span className="ab-default">✓ Mặc định</span>}<span className="pw-tag">Chưa xác minh</span></div><h2>{item.label ?? item.contactName}</h2>{item.label && <p className="ab-contact-name">{item.contactName}</p>}<p>{item.contactPhone}</p><address>{item.address.addressLine}<br />{item.address.locality}, {item.address.administrativeArea}, Việt Nam</address><div className="ab-actions"><button className="pw-primary" disabled={busy || uncertain || !!editor || !!archive} onClick={() => navigate('/personal/create?' + (item.kind === 'SENDER' ? 'senderAddressId=' : 'recipientAddressId=') + item.id)}>Dùng tạo vận đơn</button><button className="pw-secondary" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { setEditor({ original: item }); setError(''); setSuccess(''); }}>Chỉnh sửa</button>{!item.isDefault && <button className="sd-link" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { void execute({ operation: 'updatePersonalAddress', target: item.id, etag: '"v' + item.version + '"', input: { ...inputOf(item), isDefault: true } }); }}>Đặt mặc định</button>}<button className="ab-archive-button" disabled={busy || uncertain || !!editor || !!archive} onClick={() => { setArchive(item); setError(''); setSuccess(''); }}>Lưu trữ</button></div></article>)}</div>}
      <nav className="ab-pagination" aria-label="Phân trang sổ địa chỉ"><button className="pw-secondary" disabled={page === 0 || busy || uncertain} onClick={() => setPage(value => value - 1)}>Trang trước</button><span>Trang {page + 1}</span><button className="pw-secondary" disabled={!data.hasNext || busy || uncertain} onClick={() => setPage(value => value + 1)}>Trang sau</button></nav>
    </>}
  </section>;
}
