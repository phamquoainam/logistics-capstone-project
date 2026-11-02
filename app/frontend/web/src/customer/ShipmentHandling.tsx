import { useEffect, useRef, useState } from 'react';
import { customerClient, CustomerApiError } from './client';
import type { PilotHandling, PilotShipment } from './client';

export function PrivateLabel({ shipment, station }: { shipment: string; station?: string }) {
  return <PreparedLabel key={shipment + ':' + (station ?? 'personal')} shipment={shipment} station={station} />;
}
function PreparedLabel({ shipment, station }: { shipment: string; station?: string }) {
  const [busy, setBusy] = useState(false), [prepared, setPrepared] = useState<{ shipment: string; station?: string; url: string }>(), [error, setError] = useState('');
  const epoch = useRef({ generation: 0 }), url = prepared?.shipment === shipment && prepared.station === station ? prepared.url : '';
  useEffect(() => { const lifecycle = epoch.current;lifecycle.generation++; return () => { lifecycle.generation++; }; }, [shipment, station]);
  useEffect(() => () => { if (prepared) URL.revokeObjectURL(prepared.url); }, [prepared, shipment, station]);
  async function prepare() { const current = epoch.current.generation;setBusy(true);setError(''); try { const pdf = await customerClient.downloadPilotLabel(shipment, station);if (current === epoch.current.generation) setPrepared({ shipment, station, url: URL.createObjectURL(pdf) }); } catch { if (current === epoch.current.generation) setError('Không thể tải nhãn. Hãy kiểm tra phiên và quyền truy cập rồi thử lại.'); } finally { if (current === epoch.current.generation) setBusy(false); } }
  return <div className="sl-label"><p>Nhãn mô phỏng · 100 × 150 mm · Một nhãn cho mỗi kiện · Code128</p>{error && <p role="alert">{error}</p>}<div className="sl-actions"><button className="pw-primary" disabled={busy} onClick={() => { void prepare(); }}>{busy ? 'Đang tạo PDF…' : 'Tải / In nhãn'}</button>{url && <><a className="pw-secondary" href={url} download={'argopost-simulation-' + shipment + '.pdf'}>Lưu PDF</a><a className="pw-secondary" href={url} target="_blank" rel="noopener noreferrer">Mở bản in PDF</a></>}</div><small>Nhãn không phải biên nhận Carrier, POD hoặc chứng từ đã thu tiền.</small></div>;
}
export function useShipmentHandling(id: string, enabled = true) {
  const [value, setValue] = useState<PilotHandling>(), [error, setError] = useState(''), [revision, setRevision] = useState(0);
  useEffect(() => { if (!enabled) return;let active = true;customerClient.readPilotHandling(id).then(v => { if (active) { setValue(v);setError(''); } }).catch(e => { if (active) { setValue(undefined);setError(e instanceof CustomerApiError && e.status === 401 ? 'Phiên đã hết hiệu lực.' : 'Chưa tải được dữ liệu tiếp nhận. Không suy đoán trạng thái.'); } });return () => { active = false; }; }, [id, enabled, revision]);
  return { value, error, reload: () => setRevision(v => v + 1) };
}
export function HandlingBadge({ id }: { id: string }) { const source = useShipmentHandling(id), last = source.value?.events.at(-1);return <div className="sl-status"><span className="pw-tag">{source.error ? 'Trạng thái chưa tải được' : !source.value ? 'Đang tải trạng thái…' : source.value.state === 'AT_STATION' ? 'Carrier đã nhận tại Station · Mô phỏng' : 'Chờ bàn giao mô phỏng'}</span>{source.value?.labelAvailable && <span className="pw-tag">Nhãn kiện sẵn sàng</span>}{last && <small>{last.stationCode} · {new Date(last.occurredAt).toLocaleString('vi-VN')}</small>}</div>; }
export function HandlingDetail({ shipment, tab }: { shipment: PilotShipment; tab: number }) {
  const source = useShipmentHandling(shipment.id), h = source.value;
  if (source.error) return <div className="cp-notice cp-error"><p role="alert">{source.error}</p><button className="pw-secondary" onClick={source.reload}>Tải lại trạng thái tiếp nhận</button></div>;
  if (!h) return <p role="status">Đang tải dữ liệu nhãn và tiếp nhận…</p>;
  const status = h.state === 'AT_STATION' ? 'Đã tiếp nhận tại Station' : 'Chờ bàn giao tại Station';
  if (tab === 1) return <><h2>Định danh & tiếp nhận kiện</h2>{h.parcels.map(p => { const facts = shipment.parcels[p.parcelNumber - 1].facts;return <article className="sd-parcel" key={p.parcelId}><h3>Kiện {p.parcelNumber} · {facts.description}</h3><code className="sl-code">{p.barcode}</code><p>{facts.weightGrams} g · {facts.lengthMm} × {facts.widthMm} × {facts.heightMm} mm · Số lượng {facts.quantity}</p><p>Giá trị khai báo: {facts.declaredValueVnd.toLocaleString('vi-VN')} VND</p><p>{p.state === 'AT_STATION' ? 'Carrier đã tiếp nhận mô phỏng' : 'Chưa được Carrier tiếp nhận'}</p></article>; })}<PrivateLabel shipment={shipment.id} /></>;
  if (tab === 5) return <><h2>Nhãn vận chuyển</h2><PrivateLabel shipment={shipment.id} /></>;
  if (tab === 3) return <><h2>{status}</h2><p>Điểm bàn giao: <strong>{shipment.quote.route.dropOffCode}</strong> · Chỉ drop-off mô phỏng, không phải địa điểm vận hành thật.</p><p>In nhãn, dán đúng từng kiện và bàn giao đủ {shipment.parcels.length} kiện để nhân viên được phân công kiểm tra.</p><p>Pickup chưa hỗ trợ. Tiếp nhận tại Station không đồng nghĩa đang vận chuyển hoặc đã giao.</p>{h.events.map(e => <p key={e.id}>{new Date(e.occurredAt).toLocaleString('vi-VN')} · {e.stationCode} · Đã tiếp nhận mô phỏng</p>)}<PrivateLabel shipment={shipment.id} /></>;
  return <><h2>{tab === 7 ? 'Lịch sử nghiệp vụ' : 'Hành trình vận đơn'}</h2><ol className="sl-timeline"><li><strong>Booking đã xác nhận</strong><time>{new Date(shipment.createdAt).toLocaleString('vi-VN')}</time><span>Nghĩa vụ UNPAID · Chưa thu tiền</span></li>{h.events.map(e => <li key={e.id}><strong>Carrier tiếp nhận tại Station</strong><time>{new Date(e.occurredAt).toLocaleString('vi-VN')}</time><span>{e.stationCode} · {h.parcels.length} kiện · Hoạt động mô phỏng</span></li>)}</ol>{h.events.length === 0 && <p>Chưa có sự kiện Carrier tiếp nhận được ghi nhận.</p>}<p>Chưa có chặng vận chuyển hoặc giao hàng. Không có sự kiện được tạo từ giao diện.</p></>;
}
