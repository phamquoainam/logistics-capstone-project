import { Link } from 'react-router-dom';

/** Menu 2 consumes existing module routes. A view selector never grants domain authority. */
export default function MerchantMenu({ current, locked = false }: { current: 'applications' | 'review' | 'directory' | 'lifecycle' | 'owner-transfer'; locked?: boolean }) {
  const entries = [
    { id: 'applications', label: 'Merchant Applications', to: '/admin/merchants/applications' },
    { id: 'review', label: 'Review Workspace', to: '/admin/merchants/applications?view=review' },
    { id: 'directory', label: 'Merchant Directory', to: '/admin/merchants/organizations' },
    { id: 'lifecycle', label: 'Organization Lifecycle', to: '/admin/merchants/organizations?view=lifecycle' },
    { id: 'owner-transfer', label: 'Owner Transfer', to: '/admin/merchants/organizations?view=owner-transfer' },
  ];
  return <nav className="iw-tabs ap-merchant-menu" aria-label="Merchant & Phê duyệt">{entries.map(entry => locked ? <span key={entry.id} aria-disabled="true" aria-current={current === entry.id ? 'page' : undefined}>{entry.label}</span> : <Link key={entry.id} to={entry.to} aria-current={current === entry.id ? 'page' : undefined}>{entry.label}</Link>)}</nav>;
}
