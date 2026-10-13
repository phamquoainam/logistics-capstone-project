# Security & Privacy Threat Controls

**Nguồn:** [REPORT] NFR-S01–S05, §2.1, BR-16–18, BR-20; các biện pháp công cụ cụ thể là [PROPOSED].

## Identity / Access
- Password salted hash bằng thuật toán uy tín (bcrypt/Argon2 theo stack đã chốt); đăng nhập có throttling, brute-force protection, session revocation.
- RBAC + object scope checks: merchant ownership, warehouse/hub membership, dispatcher assigned scope, shipper active assignment, Finance authority.
- No cross-tenant data in public tracking, export, search or dashboard aggregates. Public endpoints redact receiver name, full address, contact, financial data.
- Admin does not automatically act as Finance; approver khác shipper nộp COD.

## Secret / Payment
- `.env.example` chỉ placeholders; không commit JWT keys, VNPay sandbox keys, DB password, storage credentials.
- VNPay: verify signature/amount/reference/status from trusted callback and provider confirmation where necessary; browser redirect not ground truth.
- Financial operations use unique business ref + database transaction + audit.

## Files / Location
- POD storage private, MIME/content/size validation, authorization on upload/read, expiring signed URL nếu chốt provider.
- GPS only active location session; record original timestamps/accuracy; enforce staleness and retention policy, disable upon end of assignment/session.

## Logging/Recovery
- Mask credentials, auth header, detailed GPS and PII; audit operator actions separately from debug log.
- Reprocessing failed job requires permission, reason and evidence; never bypass state guards.

## Security test cases
Cross-merchant read/write; enumeration of tracking codes; invalid signature, replay callback; upload oversized/false MIME; revoked user, expired role, expired assignment; access POD outside task; GPS outside session; Finance vs Admin segregation; SQLi/XSS/CSRF appropriate to auth mode.
