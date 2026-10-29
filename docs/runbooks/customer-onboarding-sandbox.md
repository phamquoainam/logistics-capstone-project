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


Independent customer profile runtime additionally requires forward migration V11 and external `MERCHANT_CUSTOMER_PROFILE_RECEIPT_KEYS` (JSON object of retained key IDs to canonical base64-encoded32-byte HMAC keys) plus `MERCHANT_CUSTOMER_PROFILE_CURRENT_RECEIPT_KEY`. No sample secret or generated runtime default is supplied. The profile API is GET/PUT `/api/v1/customer/profile`; PUT requires current customer access/Origin/CSRF, exact `If-Match: "vN"` and UUID Idempotency-Key. Business-contact fields do not establish login/recovery authority. Current profile increment has local unit/H2 evidence; PostgreSQL18 and browser evidence remain pending until linked in the execution record.


## Customer web sandbox (explicit mode)

Use `npm.cmd run build:customer` for the separate ignored `dist-customer/` artifact; default `build` retains Demo output. For an actual local HTTPS frontend run `npm.cmd run dev:customer` with externally provided `ELOGISTICS_CUSTOMER_DEV_API_ORIGIN` (loopback HTTPS backend origin only), `ELOGISTICS_CUSTOMER_DEV_TLS_KEY` and `ELOGISTICS_CUSTOMER_DEV_TLS_CERT`. Supply an approved test CA via the normal Node trust mechanism where required; proxy certificate verification stays enabled. Backend `CUSTOMER_SECURITY_ORIGIN` must equal the exact frontend HTTPS origin, including port. No reverse-proxy TLS/forwarded-header bypass is introduced. The static host must serve SPA routes with no-store/no-referrer protection.

Implemented frontend routes in this increment: `/register`, `/verify-email`, `/login`, `/recover`, `/recover/confirm`, `/personal`, `/personal/profile`. These use the customer HTTP APIs, memory-only access and synchronous proof-fragment removal; no public Merchant Owner grant. Customer-profile UNKNOWN resolves only through current-self `GET /api/v1/onboarding-receipts/updateCustomerProfile/{idempotencyKey}`. Missing or unavailable receipt remains UNKNOWN. Other receipt operations and Merchant routes are not claimed implemented.

`npm.cmd run test:customer-ui` builds the customer artifact and runs seven **isolated UI unit** cases in Edge with explicit intercepted API fixtures. Its owned loopback static server has no successful API implementation and stops via an ephemeral unlogged ownership nonce. This evidence verifies forms/navigation/privacy/cross-tab/UNKNOWN states only; it is not real backend/PostgreSQL full-stack acceptance. Real UI+backend+PG18/email E2E remains a required pending gate.


The required PG18 CI job additionally builds both web modes and runs the real customer-browser fixture inside the existing owned Identity Testcontainers suite. The fixture's test-only Boot host uses production configurations and the runtime DB principal, actual HTTPS and scheduled SMTP. The nonce-bound inbox observation endpoint is only an out-of-band test operator, never an application API. Generated test CA/SPKI trust is restricted to this loopback fixture; Node independently validates the CA/hostname and wrong-host rejection, and browser ignoreHTTPSErrors remains false. Child process receives no PG/migration credentials. Sanitized `customer-browser-results.json` is the only additional runtime artifact; raw proofs, browser/host diagnostics and keystore are never uploaded. The runtime task remains pending until the linked exact-revision CI actually passes all8 cases and parent DB/cleanup assertions.

The reauthentication increment requires forward V12 before enabling the current customer binary; startup checks the new receipt relation read-only and fails closed on an older schema. POST /api/v1/customer-auth/reauthentication requires the current customer bearer, exact Origin, bound CSRF and UUID Idempotency-Key, with only email/password fields. It returns the original15min proof reference; it cannot grant business authority or renew consumed proof through retry. Existing customer login quotas also cover this password check. Old SYSTEM tables/endpoints remain compatible; never repair or rewrite V1-V11.

Owner approved private filesystem plus real ClamAV for sandbox in [sandbox evidence adapter decision](../decisions/spec003-sandbox-evidence-adapters.md). Implementations and real screening E2E remain pending; no CLEAN or production-provider readiness is claimed from this configuration decision.

## Public website and phone-contact increment

The customer-mode build now serves the public E-Logistics website at `/`, with `/services`, `/tracking`, `/quote`, `/coverage`, `/stations`, `/news`, `/faq`, `/support`, `/about`, and policy placeholders. Tracking, price, coverage and station lookup show an explicit pending state because their authoritative APIs are outside this increment. The site uses its own SVG illustration; no Viettel Post asset, outcome statistic or testimonial is presented as E-Logistics data.

Forward migration V15 adds a nullable E.164 phone to pending enrollment and a separate `identity.customer_phone_contact` row when email proof activates the account. New website enrollment requires an international number such as `+84901234567`; the server removes spaces, parentheses and hyphens and validates E.164. Existing enrolled accounts remain valid. This phone is `UNVERIFIED`, deliberately non-unique, and cannot be used for login, recovery, email association or Merchant authority. Password confirmation stays in the browser only. Apply V15 with the migration-owner job before running this binary; never repair or alter V1–V14.

The original SVG is available through `GET /api/v1/public-media/hero-network.svg`. The public media chain permits only that GET; the key is fixed, no anonymous upload exists, and private evidence remains in its separate adapter. Set `PUBLIC_MEDIA_MODE=local` and `PUBLIC_MEDIA_LOCAL_ROOT` to the absolute directory containing `hero-network.svg` for local development. The repository asset at `app/frontend/web/public/media/hero-network.svg` is a safe local source. The frontend uses the bundled copy if the media service is unavailable. For S3, set `PUBLIC_MEDIA_MODE=s3`, `PUBLIC_MEDIA_S3_REGION`, and `PUBLIC_MEDIA_S3_BUCKET`; place the reviewed SVG at `public/website/hero-network.svg` using an independently authorized deployment process. The SDK uses its default credential chain; no keys or bucket policy belong in Git. The bucket can remain private because the backend reads the object and serves the fixed response with `nosniff` and sandbox CSP. No live AWS upload/download is claimed until a configured environment exercises it.

## Windows local full-stack launcher (Owner-directed 2026-10-30)

Use Windows PowerShell or Command Prompt in the repository root. The tracked `local.cmd` wrapper sets execution policy only for its child PowerShell process; it does not change system policy. The one-time initialization writes independent SYSTEM/Customer RSA3072 JWKs, proof/limiter/profile keys, and a self-signed PKCS12 certificate to `%LOCALAPPDATA%\ELogistics\customer-local` with an owner-only ACL. **Never reinitialize after creating customer data:** retained keys are needed to validate existing sessions, proofs, and receipts. Back up this private directory securely before reimaging Windows. It is outside Git and is never an artifact.

```powershell
.\scripts\dev\local.cmd init       # once per Windows account only
.\scripts\dev\local.cmd gmail      # once, prompts for sender and hidden App Password; DPAPI binds to this Windows account
.\scripts\dev\local.cmd doctor     # Java, Node, Docker, AWS, certificate, config
.\scripts\dev\local.cmd migrate    # optional independent PG18/Flyway check
.\scripts\dev\local.cmd start      # build, controlled migration, HTTPS backend and Vite readiness
.\scripts\dev\local.cmd status
.\scripts\dev\local.cmd stop       # only launcher-owned Java/Node processes, keeps PostgreSQL and volumes
```

`start` requires the existing `elogistics-d1-postgres18` setup from `infra/postgres/start-local.ps1` and its private `.demo/postgres/settings.json`; it reuses a healthy container. Maven dependencies and frontend `node_modules` must already be installed (`npm.cmd ci` in `app/frontend/web` if missing). The migration profile receives the migrator credential only for its one-shot process; the HTTPS runtime gets the runtime credential and `PG_MIGRATION_ENABLED=false`. `start` does not run Flyway repair/clean, seed accounts, or create AWS resources. AWS SDK uses `AWS_PROFILE=elogistics-dev`, bucket `elogistics-dev-media-160905`, region `ap-southeast-2`. Backend serves only the fixed public SVG from the private S3 bucket; business evidence remains on its approved filesystem sandbox adapter.

Before browser acceptance, review `%LOCALAPPDATA%\ELogistics\customer-local\localhost.crt`, then manually trust it in **Current User → Trusted Root Certification Authorities** if approved. The launcher never installs a root certificate. It reports `BROWSER_CERT_TRUST_PENDING` until this is done. Vite and Spring Boot use the same certificate, with IP SAN `127.0.0.1`; the Vite proxy validates the backend certificate through `NODE_EXTRA_CA_CERTS`. The local URLs are `https://127.0.0.1:5173/`, `/register`, `/login`, and `/recover`; backend public media is `https://127.0.0.1:8443/api/v1/public-media/hero-network.svg`.

For Gmail, use an Owner-controlled test mailbox as the registered recipient. The hidden prompt stores only a Windows DPAPI ciphertext and sender address outside Git. `LOCAL_REAL_SMTP` requires `smtp.gmail.com:587`, STARTTLS and exact sender authentication; provider acceptance is recorded as SMTP, not TEST. Verify a real registration message, explicit proof confirmation, resend, login/logout, recovery message, single-use reset and old-password denial. The script checks configuration/readiness, **not** actual inbox delivery. Do not report `LIVE SMTP VERIFIED` before the Owner observes receipt and completes those steps. Existing CI keeps its `TEST_LOOPBACK` transport and owned browser fixture; live Gmail/AWS smoke is excluded from PR CI.

The optional synthetic S3 probe is `java -jar app/backend/target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=s3-smoke` with `AWS_PROFILE`, `PUBLIC_MEDIA_S3_REGION`, `PUBLIC_MEDIA_S3_BUCKET`, and `PUBLIC_MEDIA_LOCAL_ROOT` set. It exclusively creates and removes its own random `dev-smoke/` object, verifies integrity/SSE-S3/anonymous denial, then compares the fixed public SVG through `S3PublicMediaStore`. It does not store business metadata or grant private evidence access. No `public/` or `private/` object is deleted by the probe.

Local limitations: Terms/Privacy content awaits Owner approval; `/merchant` remains fail-closed without an ACTIVE Merchant membership contract. No production legal or Merchant acceptance is inferred from the launcher.

`/merchant` currently fails closed for every customer: V1–V15 contain no ACTIVE Merchant membership or current-organization authorization API. The route shows a clear unavailable state, never a private Merchant shell or self-grant. The legal privacy/terms pages also remain explicit placeholders pending Owner-approved policy text. Do not treat these UI placeholders as complete Merchant or legal acceptance.

Run `npm.cmd run build:customer` or `npm.cmd run dev:customer` with the HTTPS settings above. `npm.cmd run test:customer-ui` exercises public navigation, responsive layout, auth forms, phone normalization, and Merchant denial using isolated API fixtures. The required real HTTPS/SMTP/PG18 browser path runs under the repository's `testcontainers-postgres` gate; local Task C runs require a fresh `TASK_C_EXECUTION_ID`, fresh `TASK_C_PRIVATE_DIRECTORY`, and a working Docker daemon. The CI job supplies these and records sanitized evidence.
