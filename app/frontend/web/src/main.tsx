import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import App from './App';
import CustomerApp from './customer/CustomerApp';
import { customerClient } from './customer/client';
import { captureEntryProof } from './customer/entry-proof';
import { captureTeamInvitation } from './customer/team-invitation-proof';
import StaffApp from './customer/StaffApp';
import { captureStaffInvitation } from './customer/staff-invitation-proof';
import AdminApp from './admin/AdminApp';
import { captureAdminInvitation } from './admin/invitation';
import './styles.css';
const mode = import.meta.env.VITE_PORTAL_MODE ?? 'demo';
if (!['demo', 'customer'].includes(mode)) throw new Error('PORTAL_MODE_INVALID');
if (mode === 'customer') {
  if (window.location.pathname === '/admin' || window.location.pathname.startsWith('/admin/')) {
    captureAdminInvitation(window.location, window.history);
    createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><AdminApp /></BrowserRouter></StrictMode>);
  } else if (window.location.pathname.startsWith('/staff/')) {
    captureStaffInvitation(window.location, window.history);
    createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><StaffApp /></BrowserRouter></StrictMode>);
  } else {
  captureTeamInvitation(window.location, window.history);
  captureEntryProof(window.location, window.history);
  void customerClient.initialize();
  createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><CustomerApp /></BrowserRouter></StrictMode>);
  }
} else {
  createRoot(document.getElementById('root')!).render(<StrictMode><HashRouter><App /></HashRouter></StrictMode>);
}
