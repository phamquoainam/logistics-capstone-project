# Production Identity security deployment and key lifecycle

Scope: ADR-0012's opt-in bounded HTTP revision. This is NOT complete Production Identity readiness.
No live credential, ACL, database, bootstrap, proxy, sender or deployment action is performed by
this runbook's publication. T-002-06/Feature002 remain incomplete.

## Required external configuration

Use identity,postgres profiles; never combine identity with demo or migration. Preapply the
accepted migrations through the separately authorized migrator; runtime has only its existing
least-privilege DataSource. Do not import Demo accounts/keys or run production bootstrap here.

Required properties (NO values/keys are supplied in repository):

- identity.security.issuer: exact non-Demo production issuer.
- identity.security.audience: exact single production browser audience.
- identity.security.current-kid: active configured ASCII key identifier.
- identity.security.jwk-set: protected external JSON JWK set; RSA, alg=RS256, use=sig. Only the
  active kid carries private material; retired entries contain public material only.
- identity.security.origin: exact HTTPS scheme/host/optional port, without trailing path,
  userinfo, query, fragment or wildcard.

Provision RSA3072+ keys OUTSIDE application startup/repository through the owner's secure
operational process. Supply a protected mount/configtree or equivalent reviewed secret injection.
Keep parent directory/file ownership and ACL restricted to operator/service; do not place material
in .demo, tracked files, shell arguments, PR text, logs or public reports. Do not enable configuration
value logging, request-header logging, actuator env/configprops or debug/trace for these secrets.
Invalid/missing keys, mismatched active key, weak keys and wrong profiles fail configuration with
fixed redacted codes. No random startup keys, local credential fallback or outbound JWKS fetch.

## Planned rotation

1. Prepare a new independent key/kid externally; never reuse a kid with different material.
2. Distribute its PUBLIC verifier to all validators while retaining the old verifier. Current
   signing kid remains unchanged in this preparation phase.
3. In a controlled rolling deployment switch active signing to the new private key. Remove the
   old private signing key from each updated instance; keep its public verifier on all instances.
4. Confirm every signer has switched. Retain the old verifier until every legitimate old token
   has expired: at least900 seconds after the last old issuance plus deployment/clock uncertainty.
   The decoder permits zero expiry grace. Operation/retention evidence is owner-managed; unit
   rotation tests do not prove rollout completion.
5. Remove retired verifier only after that gate. Configured restarts use stable provisioned keys,
   never newly generated identities. No public reload/signing/key-management endpoint exists.

## Emergency compromise

Treat suspected key compromise as an owner security incident. Replace compromised signing
material externally; remove the compromised verifier consistently from all validators, accepting
that its tokens are immediately denied. Preserve sanitized incident evidence and use approved
session/account revocation procedures where applicable. Never log key/token contents, run
unscoped DB updates, invoke bootstrap or bypass self/last-admin guards from an HTTP adapter.
This runbook does not authorize executing emergency operations on developer/production resources.

## HTTP deployment and limitations

Direct HTTPS is mandatory. The code trusts servlet isSecure from the configured server, not
client X-Forwarded-Proto/Forwarded headers. Initial profile installs no forwarded-header resolver;
proxy TLS termination/address trust needs an explicit reviewed deployment allowlist and tests.
Do not set framework forwarding trust to accept arbitrary senders. Same-origin browser reads use
Bearer access in memory; supplied Origin must exactly match. No cookie/query/Demo fallback.

Eight accepted account/administration operations are installed; browser authentication, CSRF
handshake/cookies, issuance/resend/activation, Recovery and UI are unavailable in this revision.
Use only authorized existing session fixtures in isolated tests. There is no deployed login or
public token minting endpoint, no default administrator and no token-generation command here.
Do not represent test signing fixtures as an operational login or production-bootstrap procedure.

Limiter thresholds are approved in ADR-0012; shared persistence/ACL is not. Existing Redis probe
credentials are read-only/probe-only and must NOT be reused for mutable limiters. No per-JVM
counter, spoofable forwarded-IP key or unavailable-storage fail-open is permitted. Invitation
bare-verifier compatibility remains unresolved; never scan proofs or expose selector as authority.

Verification evidence: [T-002-06 checkpoint](../implementation/t-002-06-http-security-evidence.md).

## Authentication deployment prerequisites

Apply owner-authorized V6 with migrator before starting the runtime; never change
V1-V5 or run repair. Supply identity.security.limiter-hmac through protected secret
injection, independent of JWT and invitation keys. No value is provided here.
[ADR-0013](../architecture/decisions/ADR-0013-identity-browser-authentication-limiter.md)
defines coordinated rotation (all instances,3660s quiescence), bounds and cleanup.
GET CSRF before login and after login/logout rebinding; keep access only in memory.
Refresh is single-flight; lost rotation response requires reauthentication.
503 is dependency failure,429 verified quota; neither authorizes permissive retry.
Logout503 cannot be presented as confirmed server revocation. Direct HTTPS only;
forwarded headers do not establish TLS/client identity. No deployment is executed.
