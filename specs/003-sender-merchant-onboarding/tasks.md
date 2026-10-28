# Tasks 003 — sender-merchant-onboarding

IN_PROGRESS under the Owner full implementation authorization. Per-BC and full-spec gates remain unchecked until their complete evidence passes. See the execution checklist below for completed internal tasks.

## REQ-V3-BC-007 / BC-007: Dùng tài khoản với Merchant membership ACTIVE

- [ ] **T-BC-007-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-007 và OpenAPI; fixtures S003-CONTEXT;S003-REVOKE-FIRST;S003-MUTATION-FIRST;S003-SUSPEND. Không thay accepted002 contract im lặng.
- [ ] **T-BC-007-DB** — Implement/reuse đúng boundary merchant.membership;identity.session theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-007-BE** — Implement exact operations getCustomerSubject;listCustomerContexts;selectCustomerContext;getMerchantOrganization trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-007-UI** — Integrate current web pages /contexts;/merchant/:orgId/profile với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-007-VERIFY** — Execute S003-CONTEXT;S003-REVOKE-FIRST;S003-MUTATION-FIRST;S003-SUSPEND cùng AC-BC-007-SUCCESS/AC-BC-007-DENY/AC-BC-007-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-007-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-008 / BC-008: Quản lý hồ sơ và nhân viên Merchant

- [ ] **T-BC-008-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-008 và OpenAPI; fixtures S003-CONTEXT;S003-DELEGATION;S003-DOWNLOAD. Không thay accepted002 contract im lặng.
- [ ] **T-BC-008-DB** — Implement/reuse đúng boundary merchant.organization;merchant.membership theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-008-BE** — Implement exact operations getMerchantOrganization;listMerchantMembers trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-008-UI** — Integrate current web pages /merchant/:orgId/profile;/merchant/:orgId/staff với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-008-VERIFY** — Execute S003-CONTEXT;S003-DELEGATION;S003-DOWNLOAD cùng AC-BC-008-SUCCESS/AC-BC-008-DENY/AC-BC-008-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-008-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.


## REQ-V3-BC-020 / BC-020: Truy cập qua membership Merchant

- [ ] **T-BC-020-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-020 và OpenAPI; fixtures S003-CONTEXT;S003-REVOKE-FIRST. Không thay accepted002 contract im lặng.
- [ ] **T-BC-020-DB** — Implement/reuse đúng boundary merchant.membership;identity.session theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-020-BE** — Implement exact operations getCustomerSubject;listCustomerContexts;selectCustomerContext;getMerchantOrganization trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-020-UI** — Integrate current web pages /contexts;/merchant/:orgId/profile với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-020-VERIFY** — Execute S003-CONTEXT;S003-REVOKE-FIRST cùng AC-BC-020-SUCCESS/AC-BC-020-DENY/AC-BC-020-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-020-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.


## REQ-V3-BC-032 / BC-032: Duyệt/giám sát Merchant theo policy

- [ ] **T-BC-032-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-032 và OpenAPI; fixtures S003-SELF-REVIEW;S003-REPRESENTATIVE;S003-BUSINESS-DUPLICATE;S003-APPROVAL-RACE;S003-AUDIT-FAIL;S003-ACTIVATION-FAIL;S003-OWNER-RECOVERY;S003-CLOSURE;S003-SUSPEND. Không thay accepted002 contract im lặng.
- [ ] **T-BC-032-DB** — Implement/reuse đúng boundary merchant.application;merchant.organization;merchant.membership;merchant.business_audit theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-032-BE** — Implement exact operations getReviewOrganization;getReviewOwnerRecovery;uploadMerchantLegalEvidence;listReviewApplications;getReviewApplication;takeApplicationReview;requestApplicationInformation;approveApplication;rejectApplication;suspendMerchantOrganization;reinstateMerchantOrganization;closeMerchantOrganization;requestMerchantOwnerRecovery;approveMerchantOwnerRecovery;createOwnerRecoveryConsent;reviewMerchantLegalProfile;getMerchantLegalProfileRevision trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-032-UI** — Integrate current web pages /business-review/applications;/business-review/applications/:id;/business-review/organizations/:orgId với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-032-VERIFY** — Execute S003-SELF-REVIEW;S003-REPRESENTATIVE;S003-BUSINESS-DUPLICATE;S003-APPROVAL-RACE;S003-AUDIT-FAIL;S003-ACTIVATION-FAIL;S003-OWNER-RECOVERY;S003-CLOSURE;S003-SUSPEND cùng AC-BC-032-SUCCESS/AC-BC-032-DENY/AC-BC-032-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CONTEXT-02;POLICY-003-01;CLOSE-OWNERS là thực, không fake. Trace TEST-V3-BC-032-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-045 / BC-045: Sửa nội dung nghiệp vụ Merchant

- [ ] **T-BC-045-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-045 và OpenAPI; fixtures S003-ADMIN-DENY. Không thay accepted002 contract im lặng.
- [ ] **T-BC-045-DB** — Không DDL hoặc positive Merchant write cho DENY045; PG before/after chứng minh zero business effects.
- [ ] **T-BC-045-BE** — Deny Identity Admin-only business command bằng production HTTP/current authority; không thêm positive capability.
- [ ] **T-BC-045-UI** — Không business-edit action cho Identity Admin-only; direct request vẫn deny.
- [ ] **T-BC-045-VERIFY** — Execute S003-ADMIN-DENY cùng DENY_ONLY/AC-BC-045-DENY/AC-BC-045-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-045-DENY với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-132 / BC-132: Đi đến đăng ký một tài khoản khách hàng cá nhân

- [ ] **T-BC-132-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-132 và OpenAPI; fixtures S003-ENROLL;S003-PHONE. Không thay accepted002 contract im lặng.
- [ ] **T-BC-132-DB** — Implement/reuse đúng boundary NO_BUSINESS_WRITE theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-132-BE** — Implement exact operations getCustomerCsrf trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-132-UI** — Integrate current web pages /register;/login;/recover với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-132-VERIFY** — Execute S003-ENROLL;S003-PHONE cùng AC-BC-132-SUCCESS/AC-BC-132-DENY/AC-BC-132-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CUSTOMER-01 là thực, không fake. Trace TEST-V3-BC-132-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-133 / BC-133: Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập

- [ ] **T-BC-133-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-133 và OpenAPI; fixtures S003-APPLICATION;S003-RESUBMIT;S003-WITHDRAW;S003-UPLOAD;S003-OVERSIZE;S003-MIME;S003-QUOTA-RACE;S003-SCANNER;S003-DOWNLOAD;S003-RETENTION;S003-UNKNOWN;S003-PRIVACY. Không thay accepted002 contract im lặng.
- [ ] **T-BC-133-DB** — Implement/reuse đúng boundary merchant.application;merchant.application_revision;merchant.application_evidence theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-133-BE** — Implement exact operations createApplicationDraft;listOwnApplications;getOwnApplication;updateApplicationDraft;submitApplication;resubmitApplication;withdrawApplication;uploadApplicationEvidence;getApplicationEvidence;downloadApplicationEvidence;removeDraftEvidence;getOnboardingReceipt trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-133-UI** — Integrate current web pages /personal/applications;/personal/applications/:id với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-133-VERIFY** — Execute S003-APPLICATION;S003-RESUBMIT;S003-WITHDRAW;S003-UPLOAD;S003-OVERSIZE;S003-MIME;S003-QUOTA-RACE;S003-SCANNER;S003-DOWNLOAD;S003-RETENTION;S003-UNKNOWN;S003-PRIVACY cùng AC-BC-133-SUCCESS/AC-BC-133-DENY/AC-BC-133-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies POLICY-003-01;PRIVACY-01 là thực, không fake. Trace TEST-V3-BC-133-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-134 / BC-134: Đăng ký/đăng nhập tài khoản cá nhân

- [ ] **T-BC-134-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-134 và OpenAPI; fixtures S003-ENROLL;S003-DUPLICATE;S003-VERIFY;S003-PROOF-REPLAY;S003-PHONE;S003-LOGIN;S003-REFRESH;S003-LOGOUT;S003-RECOVERY;S003-ENUMERATION;S003-DELIVERY;S003-ABUSE;S003-DEPENDENCY-OUTAGE. Không thay accepted002 contract im lặng.
- [ ] **T-BC-134-DB** — Implement/reuse đúng boundary identity.account;identity.customer_enrollment;identity.customer_identifier;identity.customer_proof;identity.session theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-134-BE** — Implement exact operations getCustomerCsrf;enrollCustomer;resendCustomerVerification;verifyCustomerEmail;loginCustomer;refreshCustomer;logoutCustomer;getCustomerSubject;requestCustomerRecovery;confirmCustomerRecovery;requestCustomerReauthentication;requestLegacyCustomerLink;confirmLegacyCustomerLink trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-134-UI** — Integrate current web pages /register;/verify-email;/login;/recover;/recover/confirm với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-134-VERIFY** — Execute S003-ENROLL;S003-DUPLICATE;S003-VERIFY;S003-PROOF-REPLAY;S003-PHONE;S003-LOGIN;S003-REFRESH;S003-LOGOUT;S003-RECOVERY;S003-ENUMERATION;S003-DELIVERY;S003-ABUSE;S003-DEPENDENCY-OUTAGE cùng AC-BC-134-SUCCESS/AC-BC-134-DENY/AC-BC-134-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-CUSTOMER-01;PRIVACY-01 là thực, không fake. Trace TEST-V3-BC-134-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

```ownership-json
{
  "scope": "BC-134", "orchestrator": "spec003",
  "resources": [
    {"resource_id": "identity.account", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.credentials", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.verification", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.security_contact", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.session", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null}
  ]
}
```

## REQ-V3-BC-135 / BC-135: Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản

- [ ] **T-BC-135-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-135 và OpenAPI; fixtures S003-PROFILE;S003-PROFILE-INJECTION;S003-EMAIL-CHANGE;S003-PG. Không thay accepted002 contract im lặng.
- [ ] **T-BC-135-DB** — Implement/reuse đúng boundary customer_profile;identity.security_contact theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-135-BE** — Implement exact operations getCustomerProfile;updateCustomerProfile;requestSecurityEmailChange;confirmSecurityEmailChange;requestBusinessContactProof;confirmBusinessContactProof trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-135-UI** — Integrate current web pages /personal/profile;/personal/security với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-135-VERIFY** — Execute S003-PROFILE;S003-PROFILE-INJECTION;S003-EMAIL-CHANGE;S003-PG cùng AC-BC-135-SUCCESS/AC-BC-135-DENY/AC-BC-135-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies DATA-01;IAM-CUSTOMER-01 là thực, không fake. Trace TEST-V3-BC-135-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

```ownership-json
{
  "scope": "BC-135", "orchestrator": "spec003",
  "resources": [
    {"resource_id": "retail_sender.business_profile", "authoritative_owner": "customer_profile", "resolution": "OWNER_POLICY_ACCEPTED", "decision": "DATA-01"},
    {"resource_id": "identity.security_contact", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null}
  ]
}
```

## REQ-V3-BC-157 / BC-157: Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant

- [ ] **T-BC-157-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-157 và OpenAPI; fixtures S003-TRANSFER;S003-LAST-OWNER;S003-CLOSURE;S003-BUSINESS-DUPLICATE. Không thay accepted002 contract im lặng.
- [ ] **T-BC-157-DB** — Implement/reuse đúng boundary merchant.organization;merchant.owner_change theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-157-BE** — Implement exact operations getMerchantOrganization;updateMerchantLegalProfile;getMerchantLegalProfileRevision;proposeMerchantOwnerTransfer;getMerchantOwnerTransfer;acceptMerchantOwnerTransfer;revokeMerchantOwnerTransfer;requestMerchantClosure;uploadMerchantLegalEvidence;getMerchantLegalEvidence;downloadMerchantLegalEvidence trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-157-UI** — Integrate current web pages /merchant/:orgId/profile;/merchant/:orgId/ownership với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-157-VERIFY** — Execute S003-TRANSFER;S003-LAST-OWNER;S003-CLOSURE;S003-BUSINESS-DUPLICATE cùng AC-BC-157-SUCCESS/AC-BC-157-DENY/AC-BC-157-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies POLICY-003-01;CLOSE-OWNERS là thực, không fake. Trace TEST-V3-BC-157-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-158 / BC-158: Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức

- [ ] **T-BC-158-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-158 và OpenAPI; fixtures S003-INVITE;S003-INVITE-WRONG;S003-INVITE-EXPIRE;S003-INVITER-REVOKE;S003-INVITE-RESEND;S003-INVITE-RACE;S003-DELEGATION;S003-LAST-OWNER. Không thay accepted002 contract im lặng.
- [ ] **T-BC-158-DB** — Implement/reuse đúng boundary merchant.membership;merchant.member_grant;merchant.membership_invitation theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-158-BE** — Implement exact operations listMerchantMembers;replaceMerchantMemberGrants;suspendMerchantMember;reinstateMerchantMember;revokeMerchantMember;leaveMerchantOrganization;listMerchantInvitations;createMerchantInvitation;resendMerchantInvitation;revokeMerchantInvitation;previewMerchantInvitation;acceptMerchantInvitation;rejectMerchantInvitation trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-158-UI** — Integrate current web pages /merchant/:orgId/staff;/merchant/:orgId/invitations với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-158-VERIFY** — Execute S003-INVITE;S003-INVITE-WRONG;S003-INVITE-EXPIRE;S003-INVITER-REVOKE;S003-INVITE-RESEND;S003-INVITE-RACE;S003-DELEGATION;S003-LAST-OWNER cùng AC-BC-158-SUCCESS/AC-BC-158-DENY/AC-BC-158-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-INVITE-03;IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-158-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-159 / BC-159: Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định

- [ ] **T-BC-159-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-159 và OpenAPI; fixtures S003-FACILITY;S003-DEFAULTS;S003-CATALOG-MISSING. Không thay accepted002 contract im lặng.
- [ ] **T-BC-159-DB** — Implement/reuse đúng boundary merchant.facility;merchant.organization_defaults theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-159-BE** — Implement exact operations listMerchantFacilities;createMerchantFacility;updateMerchantFacility;getMerchantDefaults;updateMerchantDefaults trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-159-UI** — Integrate current web pages /merchant/:orgId/facilities;/merchant/:orgId/defaults với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-159-VERIFY** — Execute S003-FACILITY;S003-DEFAULTS;S003-CATALOG-MISSING cùng AC-BC-159-SUCCESS/AC-BC-159-DENY/AC-BC-159-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies CAT-004;POL-005 là thực, không fake. Trace TEST-V3-BC-159-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-166 / BC-166: Quản lý nhân sự và quyền hạn được Owner ủy quyền

- [ ] **T-BC-166-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-166 và OpenAPI; fixtures S003-DELEGATION;S003-INVITER-REVOKE;S003-REVOKE-FIRST. Không thay accepted002 contract im lặng.
- [ ] **T-BC-166-DB** — Implement/reuse đúng boundary merchant.membership;merchant.member_grant;merchant.membership_invitation theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-166-BE** — Implement exact operations listMerchantMembers;replaceMerchantMemberGrants;createMerchantInvitation;resendMerchantInvitation;revokeMerchantInvitation;suspendMerchantMember;reinstateMerchantMember;revokeMerchantMember trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-166-UI** — Integrate current web pages /merchant/:orgId/staff;/merchant/:orgId/invitations với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-166-VERIFY** — Execute S003-DELEGATION;S003-INVITER-REVOKE;S003-REVOKE-FIRST cùng AC-BC-166-SUCCESS/AC-BC-166-DENY/AC-BC-166-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies IAM-INVITE-03;IAM-CONTEXT-02 là thực, không fake. Trace TEST-V3-BC-166-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.

## REQ-V3-BC-167 / BC-167: Điều hành cửa hàng/kho và cấu hình vận hành được giao

- [ ] **T-BC-167-CONTRACT** — Pin reviewed operation/DTO/error/authority mapping ở contracts/coverage.csv row BC-167 và OpenAPI; fixtures S003-FACILITY;S003-CATALOG-MISSING;S003-REVOKE-FIRST. Không thay accepted002 contract im lặng.
- [ ] **T-BC-167-DB** — Implement/reuse đúng boundary merchant.facility;merchant.organization_defaults theo contracts/data-model.md sau acceptance; read-only chứng minh no extra DDL. Forward-only/V1–V6 immutable; PG18 empty+upgrade+privilege+constraint/rollback/races theo row; không cross-module SQL.
- [ ] **T-BC-167-BE** — Implement exact operations listMerchantFacilities;createMerchantFacility;updateMerchantFacility;getMerchantDefaults;updateMerchantDefaults trong current backend qua approved public APIs. Áp dụng contracts/README.md admission/lock/decision-point/receipt/rollback protocol và candidate permission matrix đã accepted; không body actor/Owner authority.
- [ ] **T-BC-167-UI** — Integrate current web pages /merchant/:orgId/facilities;/merchant/:orgId/defaults với exact typed client sau acceptance; loading/empty/field/error/pending/conflict/unknown/revoked, keyboard/focus/responsive, no mock success. Scope/cache/context rules theo contracts/README.md.
- [ ] **T-BC-167-VERIFY** — Execute S003-FACILITY;S003-CATALOG-MISSING;S003-REVOKE-FIRST cùng AC-BC-167-SUCCESS/AC-BC-167-DENY/AC-BC-167-RECOVERY qua real unit/contract/HTTP/PG/browser tương ứng; dependencies CAT-004;POL-005 là thực, không fake. Trace TEST-V3-BC-167-FLOW với source/integration/merge SHA, commands/reports/digests và honest skips.


## Closure toàn Spec

- [ ] T-L003-FULL: từng REQ có AC/design/task/code/API/DB/UI/test/receipt; lint/typecheck/build/test theo app liên quan, required Architecture/H2/PG18 CI exact revision, report/privacy/owned cleanup và backward compatibility giữ nguyên; no unresolved blocking findings.
- [ ] T-L003-ACCEPT: scoped PR integration PASS → Owner merge → regression trên merge SHA → Owner full-spec acceptance riêng. Chưa tick vì docs promotion.


## Complete implementation sequence — Owner authorization granted

The focused review under Owner conditional approval has closed the planning candidate gate (contracts/technical-review.md). OWNER_SPEC003_FULL_IMPLEMENTATION: GO now covers all routine T003-01..12 steps below; see implementation.md for source and actual evidence. No task is DONE from documentation/static fixtures. Per-BC CONTRACT tasks refer to this same approval, not repeated permission requests.

| Task | Depends on | Concrete deliverable / gate |
|---|---|---|
| T003-01 | Reviewed planning baseline + one production authorization | Implement new Identity customer contract/admission delta; SYSTEM/RS256/username/password/session/CSRF/Origin/limiter/full-target regressions unchanged; new purpose/email/phone-negative/receipt fixtures. |
| T003-02 | T003-01 exact model | Forward migration groups per data-model.md; PG18 cold/V6 upgrade/privileges/uniqueness/old-build compatibility, no V1–V6 rewrite/repair. |
| T003-03 | T003-01/02, approved email adapter config before real delivery | Enrollment24h/resend/verify/activate/login/refresh/logout/recovery15min; secure outbox/delivery/maintenance/reauth/contact-change; proof replay/expiry/abuse/enumeration negatives and real filter-chain/browser. |
| T003-04 | T003-02/03, approved private storage/scanning before upload E2E | Independent customer_profile; partial draft then complete submit/new revision; business-contact attestation; scoped quarantine/screened evidence; quota races/private downloads; dry-run retention/holds, purge stays off. |
| T003-05 | T003-01/02 | Current personal/multi-org authority, contexts, permission/assignability/version API, reviewer scope bootstrap procedure; revoke-first/mutation-first separate-connection races and cache switching. |
| T003-06 | T003-04/05 | Review queue/take/info/reject/approve; representative/duplicate/self-review denial; atomic org+initial Owner+audit/receipt/outbox, interruption recovery/no partial ACTIVE. |
| T003-07 | T003-05/06 | Business invitations72h/recipient own enrollment/preview/accept/reject/expiry/resend/revoke, member rights union/delegation/suspend/reinstate/leave, inviter/policy invalidation; race/last-Owner tests. |
| T003-08 | T003-05/06/07 | Legal-profile proposed revision/independent review; org suspend/reinstate; closure pending/real clearance; target-consented transfer/recovery/two-reviewer/last-Owner race, no Identity Admin bypass. |
| T003-09 | T003-06, CAT-004/POL-005 minimum owner subset | Facility metadata and versioned defaults; real owner read/validate publication locks/fixtures first, unsupported/stale/outage deny. No pickup/Station/pricing master duplication. |
| T003-10 | T003-03..09 in sequence, real deployment test settings | All routes/forms/navigation in contracts/README.md with real backend+PG; E2E journeys for all13 CORE+deny045, REQ-AB-ENTRY/CONTEXT, malicious/security/context/recovery/accessibility states. |
| T003-11 | All above | Lint/typecheck/build/contract/unit/ArchUnit/H2/PG18/HTTP/browser, migration/compatibility/privacy/report-policy gates; exact source+PR integration evidence/digests, no unreviewed skip/guard weakening. |
| T003-12 | T003-11 | Full BC/S003/supplement/legacy-consumer traceability; required CI, Owner PR merge, merge-SHA CI, separately recorded full-Spec003 acceptance. Cross-Spec Group A–F acceptance remains separate. |

- [ ] **T003-01** - IN_PROGRESS: customer admission/session implemented; complete Identity delta remains.
- [ ] **T003-02** - IN_PROGRESS: V7-V11 recorded CI; V12 reauthentication receipt awaits PG18.
- [ ] **T003-03** - IN_PROGRESS: enrollment/session/SMTP verified; recovery correction, sensitive-contact/link and browser gates remain.
- [ ] **T003-04** - IN_PROGRESS: independent profile tasks below; application/evidence journeys remain.
- [ ] **T003-05** - NOT_STARTED: Merchant authority and contexts.
- [ ] **T003-06** - NOT_STARTED: review and safe activation.
- [ ] **T003-07** - NOT_STARTED: invitations and membership lifecycle.
- [ ] **T003-08** - NOT_STARTED: legal review, ownership and closure.
- [ ] **T003-09** - NOT_STARTED: bounded owner dependencies and defaults.
- [ ] **T003-10** - IN_PROGRESS: customer entry/recovery/profile API UI authored; all real full-stack browser journeys and remaining Merchant routes still required.
- [ ] **T003-11** - IN_PROGRESS: per-increment checks; complete-spec verification remains.
- [ ] **T003-12** - NOT_STARTED: full traceability, merge-SHA gates and separate Owner acceptance.
- [ ] **T-AB-ENTRY** — REQ-AB-ENTRY exact customer-auth contract, real entry/forms/verification/login/recovery; TEST-AB-ENTRY Guest no application submit, personal default, deep-link recheck, no grants; dependencies Identity customer amendment and approved delivery channel.
- [ ] **T-AB-CONTEXT** — REQ-AB-CONTEXT exact current-subject/org authority contracts and real context UI; TEST-AB-CONTEXT A→B stale-response isolation, revoke during submit, personal unaffected; no mocked membership authority.

Minimum dependent work and acceptance limits: contracts/minimum-dependencies.md. Complete DoR disposition: readiness.md. No requirement to complete all future logistics specs; no full003 acceptance without BC159's real minimum catalog subset and safe closure/delivery/privacy behavior.

## Execution checklist - completed internal tasks, 2026-10-28

Each checkbox closes only its stated deliverable. It does not imply acceptance of the enclosing T003 task, BC or full Spec003. Completed items are checked as soon as their evidence exists; outstanding required runtime gates stay open.

- [x] **T003-04-PROFILE-DOMAIN** - DONE: independent business-profile domain/application, current self-admission boundary, immutable account reference, optional untrusted contact metadata, optimistic versions and read-only initial profile. `CustomerProfileTests`: nine actual unit cases PASS in the clean backend run below.
- [x] **T003-04-PROFILE-RECEIPTS-UNIT** - DONE: exact operation/target/version/field HMAC, retained-key rotation, original-version retry, changed-body denial and redacted diagnostics; four `MerchantFingerprintTests` plus replay/conflict application cases PASS. This closes unit verification only; PG receipt/rollback/race gate remains below.
- [x] **T003-11-PROFILE-LOCAL** - DONE: clean backend `mvnw -o -B ... clean verify`, 612 discovered / 595 PASS / 17 expected policy skips / zero failure/error. Architecture23 including negative fixtures PASS in this full run. Actual report collector returns PASS; no PostgreSQL substitute is claimed.
- [x] **T003-04-PROFILE-PG** - DONE: all ten profile/V11 cases and required PostgreSQL18 CI PASS on source ede1e50 / integration ab498d6; run40622633558 and evidence/customer-profile-ci.json verify exact checkout, digests and cleanup. The additional receipt-lookup operation below has a separate pending gate.
- [ ] **T003-04-PROFILE-BROWSER** - Actual customer frontend profile form and running-component browser verification remain.
- [x] **T003-03-RECOVERY-PG-CORRECTION** - DONE: corrected fixture and all recovery negatives PASS in required run40622633558 on source ede1e50, with verified artifact digests and owned cleanup. Earlier failed/cancelled runs remain recorded.

- [x] **T003-10-CUSTOMER-CLIENT-UNIT** - DONE: 12 new isolated transport/privacy/replay/receipt tests plus 28 existing frontend unit cases PASS (40 total). Both Demo and Customer builds, typecheck and current source lint PASS; build output excluded from lint just as existing dist output is excluded.
- [x] **T003-04-PROFILE-RECEIPT-LOOKUP-PG** - DONE: current-self receipt lookup, foreign-subject/revoked/unknown/malformed denials and no-effect read PASS in required CI40624272934; source80d8a64/integrationf33d99b, verified evidence/customer-entry-profile-ci.json.
- [x] **T003-10-ENTRY-UI-UNIT** - DONE: seven isolated Edge UI cases PASS, runner exit0 in6.8s with nonce-bound owned static-server cleanup and no remaining listener; mobile screenshot visually checked. Explicit API fixtures remain UI unit evidence only, not running-backend/PostgreSQL E2E.

- [ ] **T003-10-ENTRY-REAL-RUNTIME** - Eight serial cases authored/discovered against real HTTPS production configurations, existing owned PG18 and actual SMTP; mandatory finite CI case requires all8 PASS plus DB/no-grant/one-write/revocation/owned cleanup. Local H2/compile/policy checks PASS, actual hosted runtime execution pending. This is distinct from the completed isolated UI unit gate.

- [x] **T003-03-REAUTHENTICATION-UNIT** - DONE: nine new reauthentication application/persistence-boundary unit cases plus the new final proof-deadline admission case PASS. Focused36 cases PASS; actual clean backend623 discovered/606 PASS/17 expected skips, no failure/error. This closes the bounded unit gate only; full security-email/contact/link and Merchant proof consumers remain.
- [ ] **T003-03-REAUTHENTICATION-PG** - Five actual PostgreSQL/HTTP/quota/rollback/concurrent-consumption cases and two V12 migration/privilege cases authored and compiled; await exact current CI. V1-V11 unchanged, runtime startup fails closed without V12.
