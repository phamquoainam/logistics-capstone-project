import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter } from 'react-router-dom';
import App from './App';
import CustomerApp from './customer/CustomerApp';
import { customerClient } from './customer/client';
import { captureEntryProof } from './customer/entry-proof';
import './styles.css';
const mode = import.meta.env.VITE_PORTAL_MODE ?? 'demo';
if (!['demo', 'customer'].includes(mode)) throw new Error('PORTAL_MODE_INVALID');
if (mode === 'customer') {
  captureEntryProof(window.location, window.history);
  void customerClient.initialize();
  createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><CustomerApp /></BrowserRouter></StrictMode>);
} else {
  createRoot(document.getElementById('root')!).render(<StrictMode><HashRouter><App /></HashRouter></StrictMode>);
}
