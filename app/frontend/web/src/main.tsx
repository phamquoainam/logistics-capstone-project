import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import App from './App';
import CustomerApp from './customer/CustomerApp';
import { customerClient } from './customer/client';
import { captureBusinessContactProof } from './customer/business-contact-proof';
import { captureEntryProof } from './customer/entry-proof';
import { captureTeamInvitation } from './customer/team-invitation-proof';
import { captureStaffInvitation } from './customer/staff-invitation-proof';
import { captureAdminInvitation } from './admin/invitation';
import InternalApp from './internal/InternalApp';
import './styles.css';
const mode = import.meta.env.VITE_PORTAL_MODE ?? 'demo';
if (!['demo', 'customer'].includes(mode)) throw new Error('PORTAL_MODE_INVALID');
if (mode === 'customer') {
  const path = window.location.pathname;
  if (['/admin', '/ops', '/finance', '/staff'].some(prefix => path === prefix || path.startsWith(prefix + '/'))) {
    if (path === '/admin' || path.startsWith('/admin/')) captureAdminInvitation(window.location, window.history);
    else if (path.startsWith('/staff/')) captureStaffInvitation(window.location, window.history);
    createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><InternalApp /></BrowserRouter></StrictMode>);
  } else {
  captureTeamInvitation(window.location, window.history);
  captureBusinessContactProof(window.location, window.history);
  captureEntryProof(window.location, window.history);
  void customerClient.initialize();
  createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><CustomerApp /></BrowserRouter></StrictMode>);
  }
} else {
  createRoot(document.getElementById('root')!).render(<StrictMode><HashRouter><App /></HashRouter></StrictMode>);
}
