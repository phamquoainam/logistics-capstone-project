import { useEffect, useState } from 'react';
import type { Subject } from './client';
import { customerClient } from './client';
import type { BusinessContext } from './business-data';
import { currentTeamContext, teamCapabilityGroups, teamRoleLabels } from './organization-team';
import './organization-team.css';
import { OrganizationTeamDirectory } from './OrganizationTeamDirectory';
import { OrganizationTeamInvitations } from './OrganizationTeamInvitations';
import { OrganizationTeamAudit } from './OrganizationTeamAudit';

export function OrganizationTeam({ context, subject }: { context: BusinessContext; subject: Subject }) {
  const [revision, setRevision] = useState(0), [search, setSearch] = useState('');
  const [result, setResult] = useState<{ key: string; context?: BusinessContext; error?: boolean }>();
  const key = `${subject.accountId}:${subject.securityVersion}:${context.organizationId}:${context.membershipId}:${context.authorityVersion}:${revision}`;
  useEffect(() => {
    let active = true;
    void customerClient.businessContexts().then(values => currentTeamContext(values, context)).then(current => { if (active) setResult({ key, context: current }); }).catch(() => { if (active) setResult({ key, error: true }); });
    return () => { active = false; };
  }, [context, key]);
  const current = result?.key === key ? result : undefined, admitted = current?.context;
  const groups = admitted ? teamCapabilityGroups(admitted, search) : [];
  return <section className="otm-team"><header className="bw-heading"><div><span className="pw-eyebrow">{context.displayName}</span><h1 tabIndex={-1}>Nhân sự & Phân quyền</h1><p>Thành viên, vai trò và phạm vi truy cập trong doanh nghiệp.</p></div><button type="button" className="pw-secondary" disabled={!current} onClick={() => setRevision(v => v + 1)}>Kiểm tra lại quyền</button></header>
    {!current ? <section className="bw-panel otm-card" role="status">Đang kiểm tra Membership và quyền hiện hành…</section> : current.error ? <section className="bw-panel otm-card" role="alert"><h2>Chưa xác nhận được quyền truy cập</h2><p>Membership không còn khả dụng hoặc chưa tải được dữ liệu. Thông tin quyền cũ đã được ẩn; hãy kiểm tra lại.</p></section> : admitted && <>
      <section className="bw-panel otm-member" aria-label="Membership của tôi"><span className="otm-avatar" aria-hidden="true">{subject.displayName.trim().slice(0, 1).toLocaleUpperCase('vi')}</span><div className="otm-member-name"><h2>{subject.displayName}</h2><span>{admitted.displayName}</span></div><div className="otm-member-facts"><div><small>Vai trò</small><strong>{teamRoleLabels[admitted.role]}</strong></div><div><small>Membership</small><strong className="otm-active">Đang hoạt động</strong></div></div></section>
      {admitted.permissions.includes('team.read') && <OrganizationTeamDirectory key={admitted.organizationId + ':' + admitted.authorityVersion} context={admitted} />}
      {admitted.permissions.includes('team.invite') && ['OWNER', 'MANAGER'].includes(admitted.role) && <OrganizationTeamInvitations key={'invites:' + admitted.organizationId + ':' + admitted.authorityVersion} context={admitted} />}
      {admitted.permissions.includes('team.audit.read') && <OrganizationTeamAudit key={'audit:' + admitted.organizationId + ':' + admitted.authorityVersion} context={admitted} />}
      <div className="otm-layout"><section className="bw-panel otm-card" aria-label="Quyền của tôi"><div className="otm-section-heading"><div><h2>Quyền của tôi</h2><p>Quyền được cấp ở phạm vi toàn Organization.</p></div><span className="otm-count">{admitted.permissions.length} quyền</span></div><label className="otm-search">Tìm quyền<input type="search" maxLength={120} placeholder="Vận đơn, kho, cước, danh bạ…" value={search} onChange={event => setSearch(event.target.value)} /></label><div className="otm-capabilities">{groups.map(group => <section key={group.group}><h3>{group.group}</h3><ul>{group.items.map(item => <li key={item.permission}><span className="otm-granted" aria-hidden="true">✓</span><span>{item.label}</span></li>)}</ul></section>)}</div>{groups.length === 0 && <p role="status">Không tìm thấy quyền phù hợp với từ khóa.</p>}<p className="otm-scope-note">Danh sách này không bao gồm các quyền chỉ được cấp tại từng kho. Quyền thực hiện mỗi thao tác được Backend kiểm tra tại thời điểm yêu cầu.</p><details className="otm-technical"><summary>Thông tin Membership</summary><dl><div><dt>Mã Membership</dt><dd>{admitted.membershipId}</dd></div><div><dt>Phiên bản quyền</dt><dd>{admitted.authorityVersion}</dd></div></dl></details></section>
        <aside className="otm-aside"><section className="bw-panel otm-card"><span className="otm-aside-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"><circle cx="9" cy="8" r="3" /><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6m3 10v-3a6 6 0 0 0-2-4" /></svg></span><h2>Truy cập có kiểm soát</h2><p>Danh sách, lời mời và lịch sử quản trị hiển thị theo từng quyền được cấp.</p><p className="otm-scope-note">Thay đổi vai trò hoặc thu hồi Membership không làm thay đổi tài khoản cá nhân.</p></section><section className="bw-panel otm-card otm-boundary"><h2>Quyền hạn rõ ràng</h2><p>Vai trò không tự cấp thêm quyền. Chủ doanh nghiệp quản lý trong phạm vi được nền tảng cho phép; không phải Root Admin của ArgoPost.</p><p>Phân công công việc tại kho không thay thế quyền truy cập hoặc thẩm quyền của Carrier.</p></section></aside>
      </div>
    </>}
  </section>;
}
