import { Link } from 'react-router-dom';
import InternalShell, { SourceState } from './InternalShell';

/** No operational admission contract is inferred from SYSTEM or SUPPORT sessions. */
export default function OperationsBoundary() {
  return <InternalShell portal="ops" name="Chưa có context Operations" path="/ops"><SourceState state="UNAVAILABLE" title="Carrier Operations chưa được mở">Chưa có admission và capability registry cho phân hệ vận hành. Không sử dụng tài khoản Customer, Merchant, SYSTEM Admin hoặc SUPPORT để tự mở quyền Station/Courier. Không có dữ liệu vận hành hoặc lệnh Carrier nào được gọi từ trang này.</SourceState><Link to="/admin/login">Đăng nhập Administration Console</Link><p><Link to="/staff/login">Đăng nhập nghiệp vụ CSKH</Link></p></InternalShell>;
}
