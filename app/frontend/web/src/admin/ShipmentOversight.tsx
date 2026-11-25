import { useEffect, useRef, useState } from 'react';
import { adminSession, AdminSessionError } from './session';
import { parseShipmentOversight, queryParameters } from './data';
import type { Dashboard, MetricKey, Shipment, ShipmentOversight as Detail } from './data';

/** Snapshot-bound masked read. No Customer endpoint or private document authority. */
export default function ShipmentOversight({ row, report, metric, close }: { row: Shipment; report: Dashboard; metric: MetricKey; close: () => void }) {
  const [detail, setDetail] = useState<Detail | null>(null), [error, setError] = useState('');
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    let active = true;
    heading.current?.focus();
    const parameters = queryParameters(report.query);
    parameters.set('snapshotId', report.snapshotId); parameters.set('metric', metric);
    void adminSession.read('shipment-detail', parameters, value => parseShipmentOversight(value, row, report), row.id)
      .then(value => { if (active) setDetail(value); })
      .catch(failure => { if (active) setError(failure instanceof AdminSessionError ? failure.code : 'SERVICE_UNAVAILABLE'); });
    return () => { active = false; };
  }, [row, report, metric]);
  const time = (at: string) => new Date(at).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' });
  return <section className="ap-oversight" aria-labelledby="oversight-title">
    <div className="ap-panel-heading"><div><h3 id="oversight-title" ref={heading} tabIndex={-1}>Chi tiết giám sát vận đơn</h3><p className="ap-code">{row.code}</p></div><button type="button" className="ap-secondary" onClick={close}>Về danh sách vận đơn</button></div>
    {!detail && !error && <p role="status">Đang đối chiếu kiện hàng và hành trình…</p>}
    {error && <p className="ap-error" role="alert">{error === 'SNAPSHOT_EXPIRED' ? 'Snapshot đã thay đổi hoặc hết hạn. Làm mới số liệu trước khi mở lại chi tiết.' : error === 'ACCESS_DENIED' ? 'Quyền truy cập hiện tại không cho phép xem chi tiết này.' : error === 'RESOURCE_NOT_FOUND' ? 'Vận đơn không thuộc danh sách đóng góp được phép xem.' : 'Chưa tải được chi tiết đã xác thực. Vui lòng quay lại danh sách và thử lại.'}</p>}
    {detail && <>
      <dl className="ap-oversight-summary"><div><dt>Phạm vi sở hữu</dt><dd>{row.ownerKind === 'BUSINESS' ? 'Doanh nghiệp' : 'Cá nhân'}</dd></div><div><dt>Ngày Booking</dt><dd>{time(row.bookedAt)}</dd></div><div><dt>Trạng thái</dt><dd>{row.status === 'AT_STATION' ? 'Đã tiếp nhận' : 'Chờ bàn giao'}</dd></div><div><dt>Số kiện</dt><dd>{row.parcelCount}</dd></div></dl>
      <h4>Kiện hàng</h4><div className="ap-table-scroll"><table><thead><tr><th>Mã kiện</th><th>Khối lượng thực</th><th>Kích thước (dài × rộng × cao)</th></tr></thead><tbody>{detail.parcels.map(parcel => <tr key={parcel.id}><td className="ap-code">{parcel.code}</td><td>{parcel.actualGrams.toLocaleString('vi-VN')} g</td><td>{[parcel.lengthMm, parcel.widthMm, parcel.heightMm].map(mm => (mm / 10).toLocaleString('vi-VN')).join(' × ')} cm</td></tr>)}</tbody></table></div>
      <h4>Hành trình đã ghi nhận</h4><ol className="ap-oversight-timeline"><li><strong>Đã Booking</strong><time dateTime={row.bookedAt}>{time(row.bookedAt)}</time></li>{detail.acceptance && <li><strong>Carrier đã tiếp nhận tại {detail.acceptance.stationCode}</strong><time dateTime={detail.acceptance.occurredAt}>{time(detail.acceptance.occurredAt)}</time></li>}</ol>
    </>}
  </section>;
}
