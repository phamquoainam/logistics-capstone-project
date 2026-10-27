# Customer onboarding sandbox runtime

Scope: current Spec003 enrollment/verification/customer-session delivery increment. Full Spec003 and public rollout are not accepted. Existing SYSTEM and Demo channels retain their separate contracts. Exact implementation and CI revisions: [execution record](../../specs/003-sender-merchant-onboarding/implementation.md).

## Explicit configuration

Apply forward migrations through the existing controlled migration job first; V1–V9 must retain their checksums. The customer runtime requires PostgreSQL18 and V10, checked through runtime-readable metadata without reading Flyway history or running repair. Use the same runtime DataSource for Identity and later business orchestration. Runtime must not receive migration-owner credentials; follow the existing [PostgreSQL deployment separation](postgresql-foundation.md).

Enable `identity,customer,postgres`, never Demo/migration profiles. `CUSTOMER_ROLLOUT=SANDBOX` is mandatory. The [customer profile](../../app/backend/src/main/resources/application-customer.properties) contains external placeholders only. It enables direct HTTPS; forwarding strategies other than NONE are rejected. For an approved local test, explicitly bind `CUSTOMER_BIND_ADDRESS=127.0.0.1`; there is no automatic deployment in this task.

| Configuration group | Required external inputs |
|---|---|
| Existing SYSTEM | Existing `identity.security.*` issuer/audience/JWK set/current key/origin/limiter configuration; no change to exact usernames or realm/grant rules. |
| HTTPS | `CUSTOMER_BIND_ADDRESS`, `CUSTOMER_HTTPS_PORT`, `CUSTOMER_HTTPS_KEY_STORE`, `CUSTOMER_HTTPS_KEY_STORE_PASSWORD`; approved private PKCS12 keystore. Browser origin must exactly match the configured HTTPS origin. |
| Customer RS256 | `CUSTOMER_SECURITY_ORIGIN`, `CUSTOMER_SECURITY_ISSUER`, `CUSTOMER_SECURITY_AUDIENCE`, `CUSTOMER_SECURITY_CURRENT_KID`, `CUSTOMER_SECURITY_JWK_SET`. Issuer/audience and every retained RSA public key must be distinct from SYSTEM; RSA minimum3072, explicit RS256/signature use, current private key only. |
| Proof protection | `CUSTOMER_SECURITY_PROOF_VERIFIER_KEYS`, `CUSTOMER_SECURITY_CURRENT_PROOF_KEY`, `CUSTOMER_SECURITY_DELIVERY_ENCRYPTION_KEYS`, `CUSTOMER_SECURITY_CURRENT_ENCRYPTION_KEY`. Rings are JSON objects mapping key IDs to canonical base64 of independent externally provisioned32-byte keys; current IDs must exist. No startup key generation. |
| Quota | `CUSTOMER_SECURITY_LIMITER_HMAC`: independent32-byte nonzero canonical-base64 key, distinct from SYSTEM limiter and customer proof/delivery keys. Existing SYSTEM policies/windows remain exact; customer policies use distinct namespaces. |
| SMTP | `CUSTOMER_SMTP_MODE`, `CUSTOMER_SMTP_HOST`, `CUSTOMER_SMTP_PORT`, `CUSTOMER_SMTP_SENDER`, `CUSTOMER_SMTP_APPROVED_SENDER_DOMAINS`; approved exact domain allowlist. Username/password additionally required for TLS modes. No credentials/provider are selected by a user request. |

Supply secrets from the controlled secret/configtree mechanism. Never publish environment dumps, JWKs, proof URLs, SMTP credentials, private keys or raw test diagnostics. Retain previous proof/encryption keys while referenced live delivery/proof/receipt windows require them; removing a required key blocks delivery instead of inventing success.

SMTP modes:

- `STARTTLS`: TLS required before authentication; missing STARTTLS fails closed.
- `IMPLICIT_TLS`: encrypted connection from the start.
- `TEST_LOOPBACK`: explicitly sandbox-only, host exactly127.0.0.1 or::1, no SMTP credentials; real synthetic inbox traffic, not production deliverability evidence.

TLS modes verify the server certificate/hostname with approved trust roots and support TLS1.2/1.3. Connect/read/write timeouts are3s. There is no trust-all option or plaintext fallback. SMTP protocol/body tracing blocks sending; session debug/auth output is disabled. Adapter behavior follows [Angus SMTP documentation](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html), with versions managed by the repository's existing Boot BOM.

After required migration/configuration, the backend entry point is the existing single artifact:

```powershell
java -jar app/backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=identity,customer,postgres --debug=false --trace=false
```

This command is a deployment instruction, not evidence that a shared environment or public endpoint was deployed.

## Implemented transport and delivery semantics

Current customer HTTP operations: CSRF, enrollment, verification resend, verification confirmation, login, refresh, logout, current subject (`/api/v1/customer-auth/me`), recovery request and recovery confirmation. Other approved Spec003 operations remain pending implementation; unsupported routes do not return fabricated success. Customer cookies are `__Host-elogistics.customer.csrf` and `__Host-elogistics.customer.refresh`; Secure/HttpOnly/host-only/Path=/SameSite=Strict. Access tokens belong in memory only. Following login, obtain a new CSRF challenge through the exact customer CSRF endpoint using the returned cookies before a subsequent mutation. Refresh remains strict: one concurrent rotation, replay revokes its customer family, no grace or blind retry.

Enrollment requires direct HTTPS, exact Origin, live session-unbound customer CSRF, durable quota and UUID idempotency key. Public enrollment creates no account/grant/org before proof consumption. Uniform202 includes the request receipt, not account existence or delivery status. The message URL uses a fragment; the future browser must strip it and require explicit confirmation POST. GET never activates.

Pending password material is BCrypt12, not raw password and not an invalid V3 pending account. Proof verifier is HMAC; proof token and delivery recipient are separately AES-GCM protected with purpose/subject/time/key context. Protected intent, enrollment, audit and receipt commit atomically. Successful confirmation creates the valid verified ACTIVE account/email index with proof/enrollment consumption in one transaction, without SYSTEM grants or Merchant membership.

The worker claims a30s fenced lease in a short owned transaction, decrypts and sends after commit, then records the result in another owned transaction. Each attempt and mandatory audit is atomic. Maximum5 attempts, bounded exponential retry30/60/120/240s and60s BLOCKED retry, never past proof expiry. A stale/expired worker cannot finalize a newer lease. Crashes may duplicate the same stable Message-ID email, never proof consumption/account activation. Current worker eligibility supports pending enrollment and current verified, version-bound recovery; other account-bound purposes need their own current-authority checks before being enabled.

SMTP success records `ACCEPTED_BY_PROVIDER`, provider reference and TEST classification where configured. It does not set DELIVERED. Actual recipient consumption of the bound proof records delivery evidence. Terminal transport outcomes clear encrypted intent/recipient payloads; expiry maintenance clears expired pending credential material and revokes proofs while preserving enrollment/proof/audit/receipt history. Business-data purge remains off.

Recovery request is uniform202 for unknown/ineligible identifiers and sends only to the stored currently verified exact email binding. Its15min proof binds account security version without trusting a session/body account ID. Confirmation rechecks HMAC inside the locked persistence boundary, preserves username/contact/grants/invitations and atomically advances credential/security versions, invalidates both session families and affected recovery/session-version proofs, and writes mandatory audit/receipt. Old refresh tokens cannot restore a reset account. Confirmation only clears the customer refresh cookie after commit. New proof-scoped verification/recovery receipts survive CSRF challenge rotation; a current valid CSRF binding and exact secret-bound same-key payload remain mandatory. Different payload409; a new key cannot reconsume proof.

## Evidence boundaries

Local default/H2 verification includes real loopback SMTP, STARTTLS/implicit-TLS, trusted/untrusted certificates, missing STARTTLS,450/550 responses, stable Message-ID and wire-log negatives. TLS tests create a synthetic ephemeral certificate with the current JDK keytool, retain no keystore file, and close only owned sockets/executors/processes. This is TEST infrastructure, with no host trust-store mutation.

PG18 tests use the existing owned suites/container registry. HTTP/SMTP/V9 sourceb113464 has verified integration evidence in the execution record. New recovery/V10 cases cover real SMTP recovery/credential invalidation, proof/CSRF replay, concurrent reset, current-account denial, invitation preservation, audit rollback, HMAC-negative persistence and upgrade/privileges. Authored cases are not PASS evidence until the exact integration revision runs. Frontend/browser, sensitive contact/linking, independent profile and Merchant journeys are still required for full acceptance.
