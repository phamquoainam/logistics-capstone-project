import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminSession } from './session';
import { parsePlatform } from './data';
import type { PlatformStatus } from './data';
import { SourceState } from '../internal/InternalShell';

export function OperationalRisk({ merchant = false, work = false }: { merchant?: boolean; work?: boolean }) {
  return <section className="ap-risk" aria-label="Operational Risk"><div className="ap-panel"><h2>Rủi ro vận hành</h2><p>Chỉ hiển thị incident, risk hold hoặc escalation khi có sự kiện và policy authoritative. UNPAID không đồng nghĩa quá hạn; chờ bàn giao không đồng nghĩa vi phạm SLA.</p></div><div className="ap-v2-charts"><SourceState state="UNAVAILABLE" title="Network & SLA">Chưa có registry mạng lưới thương mại, lịch khai thác và policy SLA để xác định tồn đọng hoặc chậm chặng.</SourceState><SourceState state="UNAVAILABLE" title="Risk holds & Escalations">Chưa có nguồn risk case hoặc escalation được cấp authority. Không suy ra mức độ rủi ro từ số hồ sơ đang mở.</SourceState></div><p className="ap-work-links">{work && <Link to="/admin/account?tab=queue">Xem công việc được phân công</Link>}{merchant && <Link to="/admin?tab=merchant">Xem hàng đợi hồ sơ Merchant</Link>}</p></section>;
}

export function ServiceHealth() {
  const [value, setValue] = useState<PlatformStatus | null>(null), [error, setError] = useState(''), [refresh, setRefresh] = useState(0);
  useEffect(() => { let active = true;setValue(null);setError('');void adminSession.read('platform', new URLSearchParams(), parsePlatform).then(v => { if (active) setValue(v); }).catch(() => { if (active) setError('Không đọc được health qua quyền kỹ thuật hiện hành.'); });return () => { active = false; }; }, [refresh]);
  return <section className="ap-service-health" aria-label="Service Health"><div className="ap-v2-title"><div><h2>Tình trạng dịch vụ</h2><p>Kiểm tra endpoint Administration; không đại diện cho sức khỏe toàn nền tảng.</p></div><button className="ap-secondary" onClick={() => setRefresh(n => n + 1)}>Kiểm tra lại dịch vụ</button></div>{!value && !error && <p role="status">Đang kiểm tra API…</p>}{error && <p role="alert">{error}</p>}{value && <><section className="ap-panel"><h3>Administration API</h3><strong className="ap-health-up">{value.apiAvailability === 'UP' ? 'Đang phản hồi' : 'Chưa khả dụng'}</strong><p>Backend kiểm tra lúc {new Date(value.checkedAt).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}.</p></section><div className="ap-health-grid">{[{ title: 'Queues', source: value.queues }, { title: 'Scheduled jobs', source: value.scheduledJobs }, { title: 'Integration failures', source: value.integrationFailures }].map(item => <SourceState key={item.title} state="UNAVAILABLE" title={item.title}>{item.source.availability === 'SOURCE_NOT_IMPLEMENTED' ? 'Chưa có telemetry canonical được kết nối.' : 'Nguồn chưa có dữ liệu được phép hiển thị.'}</SourceState>)}</div></>}</section>;
}
