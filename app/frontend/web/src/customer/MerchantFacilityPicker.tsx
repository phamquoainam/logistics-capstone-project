import { useEffect, useRef, useState } from 'react';
import { customerClient, CustomerApiError } from './client';
import type { BusinessContext } from './business-data';
import type { FacilityPage, MerchantFacility } from './merchant-facilities-data';
import { facilityError } from './merchant-facilities-data';
import './business-facilities.css';

/** Explicit current authorized selection; never automatically rebinds a saved resource. */
export function MerchantFacilityPicker({ context, value, disabled, select, purpose = 'ANY' }: { purpose?: 'ANY' | 'DRAFT' | 'IMPORT'; context: BusinessContext; value: string | null; disabled: boolean; select: (site: MerchantFacility | null) => void }) {
  const [open, setOpen] = useState(false), [page, setPage] = useState(0), [revision, refresh] = useState(0), [busyKey, setBusyKey] = useState(''), [error, setError] = useState('');
  const [state, setState] = useState<{ key: string; page?: FacilityPage; error?: string }>();
  const key = [context.organizationId, context.authorityVersion, value, page, revision].join(':'), currentKey = useRef(key);
  useEffect(() => { currentKey.current = key; }, [key]);
  useEffect(() => { if (!open) return; let live = true; void customerClient.listMerchantFacilities(context, { page, state: 'ACTIVE' }).then(data => { if (live) setState({ key, page: data }); }).catch(e => { if (live) setState({ key, error: facilityError(e) }); }); return () => { live = false; }; }, [context, open, page, key]);
  async function apply(id?: string) {
    if (disabled || busyKey === key) return; const start = key; setBusyKey(start); setError('');
    try {
      const target = id ?? (await customerClient.readMerchantFacilityDefault(context)).facilityId;
      if (!target) { if (currentKey.current === start) setError('Chưa cấu hình địa điểm mặc định.'); return; }
      const site = await customerClient.readMerchantFacility(context, target);
      if (currentKey.current !== start) return;
      if (site.state !== 'ACTIVE' || purpose !== 'ANY' && !site.allowedActions.includes(purpose === 'DRAFT' ? 'CREATE_DRAFT' : 'IMPORT_DRAFTS')) throw new CustomerApiError('RESOURCE_NOT_FOUND');
      select(site); setOpen(false);
    } catch (e) { if (currentKey.current === start) setError(facilityError(e)); } finally { setBusyKey(v => v === start ? '' : v); }
  }
  const current = state?.key === key ? state : undefined, busy = disabled || busyKey === key;
  return <div className="wf-picker"><div className="wf-row-actions"><button className="pw-secondary" type="button" disabled={busy} onClick={() => { setOpen(v => !v); refresh(n => n + 1); }}>Chọn kho / địa điểm</button><button className="pw-secondary" type="button" disabled={busy} onClick={() => { void apply(); }}>Dùng địa điểm mặc định</button>{value && <button className="pw-secondary" type="button" disabled={busy} onClick={() => { if (window.confirm('Gỡ liên kết địa điểm? Thông tin người gửi đang nhập được giữ nguyên.')) select(null); }}>Gỡ liên kết địa điểm</button>}</div>{error && <p role="alert">{error}</p>}{open && <section className="wf-picker-options" aria-label="Địa điểm được phép sử dụng">{!current ? <p role="status">Đang tải địa điểm…</p> : current.error ? <p role="alert">{current.error}</p> : current.page && <>{current.page.items.map(site => <button type="button" key={site.id} disabled={busy || purpose !== 'ANY' && !site.allowedActions.includes(purpose === 'DRAFT' ? 'CREATE_DRAFT' : 'IMPORT_DRAFTS')} onClick={() => { void apply(site.id); }}><strong>{site.name}</strong><span>{site.code ?? 'Chưa có mã'} · {site.contact.address.addressLine}</span></button>)}{!current.page.items.length && <p>Không có địa điểm đang hoạt động trên trang này.</p>}<div className="wf-row-actions"><button className="pw-secondary" disabled={busy || page === 0} type="button" onClick={() => setPage(n => n - 1)}>Địa điểm trước</button><button className="pw-secondary" disabled={busy || !current.page.hasNext} type="button" onClick={() => setPage(n => n + 1)}>Địa điểm sau</button></div></>}</section>}</div>;
}
