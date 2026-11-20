export const organizationSupportViews = { help: 'Trung tâm hỗ trợ', tickets: 'Ticket doanh nghiệp', changes: 'Yêu cầu thay đổi', claims: 'Hồ sơ sự cố' } as const;
export type OrganizationSupportView = keyof typeof organizationSupportViews;
export function organizationSupportQuery(raw: URLSearchParams) {
  const view = raw.get('view') ?? 'help', shipment = raw.get('shipment'), ticket = raw.get('ticket'), intake = raw.get('intake');
  const valid = Object.hasOwn(organizationSupportViews, view) && [...raw.keys()].every(k => ['view', 'shipment', 'ticket', 'intake'].includes(k) && raw.getAll(k).length === 1)
    && (shipment === null || /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(shipment) && shipment !== '00000000-0000-0000-0000-000000000000');
  const validTicket = ticket === null || view === 'tickets' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(ticket) && ticket !== '00000000-0000-0000-0000-000000000000';
  const validIntake = intake === null || view === 'tickets' && ticket !== null && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(intake) && intake !== '00000000-0000-0000-0000-000000000000';
  return { valid: valid && validTicket && validIntake, intake, view: view as OrganizationSupportView, shipment, ticket };
}
export function organizationSupportLink(organization: string, view: OrganizationSupportView = 'help', shipment?: string) {
  const q = new URLSearchParams(); if (view !== 'help') q.set('view', view); if (shipment) q.set('shipment', shipment);
  return '/merchant/' + organization + '/support' + (q.size ? '?' + q : '');
}
