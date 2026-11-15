import type { KeyboardEvent, ReactNode } from 'react';
import type { BusinessShipmentView } from './client';
import { detailTabs, shipmentDate } from './shipment-management';
import { pilotMoney } from './PilotShipping';

const labels = ['Tổng quan', 'Kiện hàng', 'Hành trình', 'Lấy hàng & Giao hàng', 'Cước & COD', 'Chứng từ', 'Yêu cầu & Hỗ trợ', 'Lịch sử'];
export function BusinessShipmentTabs({ data, tab, change, download, busy, canLabel, history }: { data: BusinessShipmentView; tab: string; change: (value: string) => void; download: () => void; busy: boolean; canLabel: boolean; history: ReactNode }) {
  const s = data.shipment;
  function move(event: KeyboardEvent<HTMLElement>) {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    const target = event.target as HTMLElement;
    if (target.getAttribute('role') !== 'tab') return;
    event.preventDefault(); const current = detailTabs.indexOf(tab as typeof detailTabs[number]);
    const index = event.key === 'Home' ? 0 : event.key === 'End' ? 7 : (current + (event.key === 'ArrowRight' ? 1 : 7)) % 8;
    change(detailTabs[index]); (event.currentTarget.querySelectorAll<HTMLButtonElement>('[role=tab]')[index])?.focus();
  }
  return <><div className="om-detail-tabs" role="tablist" aria-label="Nội dung vận đơn doanh nghiệp" onKeyDown={move}>{detailTabs.map((value, i) => <button key={value} type="button" id={'business-tab-' + value} role="tab" aria-selected={tab === value} aria-controls={'business-panel-' + value} tabIndex={tab === value ? 0 : -1} onClick={() => change(value)}>{labels[i]}</button>)}</div>
    {tab !== 'overview' && <div className="om-info" id={'business-panel-' + tab} role="tabpanel" tabIndex={0} aria-labelledby={'business-tab-' + tab}>
      {tab === 'parcels' && <><h2>Kiện hàng đã xác nhận</h2>{s.parcels.map(p => <article className="sd-parcel" key={p.id}><h3>Kiện {p.parcelNumber} · {p.facts.description}</h3><code className="om-parcel-code">AP-P-{p.id.replaceAll('-', '')}</code><p>{p.facts.quantity} sản phẩm · {p.facts.weightGrams.toLocaleString('vi-VN')} g · {p.facts.lengthMm / 10} × {p.facts.widthMm / 10} × {p.facts.heightMm / 10} cm</p><p>Giá trị khai báo: {pilotMoney(p.facts.declaredValueVnd)}</p></article>)}</>}
      {tab === 'tracking' && <><h2>Hành trình Carrier</h2><p>Chưa có sự kiện Carrier tiếp nhận cho vận đơn Business. Không suy ra đang vận chuyển hoặc đã giao từ Booking hay chuẩn bị hàng nội bộ.</p><p>Điểm bàn giao dự kiến: <strong>{s.quote.route.dropOffCode}</strong>.</p></>}
      {tab === 'handover' && <><h2>Chuẩn bị bàn giao</h2><p>In nhãn và dán đúng mã cho {s.parcels.length} kiện, đối chiếu số kiện và trọng lượng đã khai báo.</p><dl className="se-facts"><div><dt>Điểm bàn giao</dt><dd>{s.quote.route.dropOffCode}</dd></div><div><dt>Phương thức</dt><dd>Drop-off theo tuyến đã báo giá</dd></div><div><dt>Tiếp nhận Carrier</dt><dd>Chưa ghi nhận</dd></div></dl><p>Chưa mở Pickup hoặc đợt bàn giao Business. In nhãn không tạo bằng chứng tiếp nhận hàng.</p></>}
      {tab === 'fees' && <><h2>Cước & COD</h2><dl className="se-facts"><div><dt>Hợp đồng / phiên bản</dt><dd>v{s.quote.contractVersion} · {s.quote.tariffVersion}</dd></div><div><dt>Người trả cước</dt><dd>Người gửi</dd></div><div><dt>Nghĩa vụ</dt><dd>UNPAID</dd></div></dl>{s.quote.feeLines.map(l => <div className="pb-fee-line" key={l.parcelNumber}><span>Kiện {l.parcelNumber}<small>Thực {l.actualGrams} g · Tính cước {l.chargeableGrams} g</small></span><strong>{pilotMoney(l.feeVnd)}</strong></div>)}<div className="pb-total"><span>Cước đã xác nhận</span><strong>{pilotMoney(data.obligation.amountVnd)}</strong></div><p>Giá trị khai báo không phải COD hoặc tiền đã thu. Chưa mở thu cước, COD, hoàn phí hoặc hóa đơn thuế.</p></>}
      {tab === 'documents' && <><h2>Chứng từ vận đơn</h2><p>Nhãn PDF 100 × 150 mm có mã Code128 cho từng Parcel; giữ đầy đủ trang tiếp nối và nhận diện Local/Test trên tài liệu có thể in/lưu độc lập.</p>{canLabel ? <button type="button" className="pw-primary" disabled={busy} onClick={download}>{busy ? 'Đang tải…' : 'Tải nhãn tất cả kiện'}</button> : <p>Cần quyền đọc nhãn tại kho của vận đơn.</p>}<p>Chưa có biên nhận Carrier, chứng từ thanh toán hoặc POD. Nhãn không phải bằng chứng đã bàn giao hoặc thu tiền.</p></>}
      {tab === 'support' && <><h2>Yêu cầu & hỗ trợ doanh nghiệp</h2><p>Chưa kết nối Ticket hoặc workflow yêu cầu thay đổi, hủy, giao lại theo Organization. Yêu cầu Personal không được dùng thay cho doanh nghiệp.</p><p>Không sửa trực tiếp snapshot, Tracking, nghĩa vụ cước hoặc kết quả giao hàng tại đây.</p></>}
      {tab === 'history' && <><h2>Mốc từ dữ liệu Booking</h2><ol className="om-history"><li><strong>Xác nhận Booking</strong><time dateTime={s.createdAt}>{shipmentDate(s.createdAt)}</time><p>Shipment và {s.parcels.length} Parcel đã được tạo. Nghĩa vụ UNPAID, Carrier chưa tiếp nhận.</p><code>Người tạo: {s.creatorAccountId}</code></li></ol>{history}</>}
    </div>}
  </>;
}
