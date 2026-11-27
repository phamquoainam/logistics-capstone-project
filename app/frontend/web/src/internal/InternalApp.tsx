import { useLocation } from 'react-router-dom';
import AdminApp from '../admin/AdminApp';
import StaffApp from '../customer/StaffApp';
import OperationsApp from '../customer/OperationsApp';

/** One internal application, separate canonical session/admission owners. */
export default function InternalApp() {
  const path = useLocation().pathname;
  if (path === '/admin' || path.startsWith('/admin/')) return <AdminApp />;
  if (path === '/ops' || path.startsWith('/ops/')) return <OperationsApp />;
  return <StaffApp />;
}
