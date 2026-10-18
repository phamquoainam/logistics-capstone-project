# ADR-0008: Protected credential store — explicit Phase 1 adoption

- Status: **ACCEPTED for the owner-approved architecture and Phase 1 scope**,
  2026-10-18. Final independent review2026-10-19:
  **REVIEW_PASS_READY_FOR_PR_PREPARATION**, for the bounded Phase 1 contract only.
  PCS-01/02/03/04 and PCS-RR-01 are RESOLVED for that contract; real migration
  and AC-001-04 acceptance remain blocked/open respectively.
- Baseline: develop `e5994b08aecea9f049a9ffd2b3d7bd8dad5ab540`.
- Evidence: [Phase 1](../../implementation/ac-001-04-credential-store-evidence.md).
- Original AC-001-04 and other REQ/AC are unchanged. Security acceptance OPEN.

## Decision and trust boundary

Use OS KnownFolder(LocalAppData)/ELogistics-Credentials/logistics-capstone-project.
Do not derive this boundary from an arbitrary LOCALAPPDATA override. This replaces
the *target design* of protecting individual files inside a broadly mutable
repository namespace. Existing credentials are not moved by this decision.

The stdlib Win32 backend queries the current token SID, complete ancestor
owners/applicable ACEs and canonical handle identities. It rejects untrusted
mutation rights, unknown ACE semantics, non-local volumes, reparse objects and
hard-linked files. Unknown or deny ACE types fail closed; no impersonation,
elevation, ownership changes or external security dependency. Directory handles
omit delete sharing through critical operations. ACLs are inspected on handles.

Developer, SYSTEM and Administrators are trusted in the ancestor namespace.
New private objects grant only current developer and Administrators recovery;
no grant is inferred for SYSTEM from Docker process names. Same-user malicious
code and privileged administrators are outside the isolation guarantee.

Directories are created with their protected descriptor **at CreateDirectoryW**:
developer `0x1200af` (read/list/traverse/create file/subdirectory), controlled
directory/file inheritance templates, Administrators `RCWD`. Existing objects
are not adopted by name. Files are created with an explicit protected descriptor
at CREATE_NEW and checked before their first write:

| Object | Developer | Administrators | Lifecycle |
| --- | --- | --- | --- |
| Immutable credential/commit marker | FR `0x120089` | RCWD `0x60000` | Initial exclusive handle writes; subsequent writes/deletes/replaces denied |
| Nonsecret mutable metadata | `0x13019f` read/write/delete, no execute | RCWD | Exclusive temp, atomic replace, named backup; guarded rollback |

An owner retains implicit WRITE_DAC; RCWD is trusted recovery authority, not
cryptographic isolation. Metadata rejects secret-bearing field names; this is
a misuse guard, not a general secret detector. Credentials must never be placed
in state/metadata. Caller processes remain trusted.

The development unit is `<project-root>/development`: immutable store marker,
generations/<32hex>/complete.json, metadata/active.json and sessions/<32hex>.
The marker binds owner/project/purpose; the commit inventory binds bytes and
rejects incomplete/extra/corrupt files. PostgreSQL test sessions are separate
from permanent generations. Seven historical records require owner retention
decisions, not automatic activation or database deletion.

## Phase 1 execution boundary

Existing launcher default remains `legacy`, preserving pre-migration operation.
`-CredentialSource protected` is explicit, validated and never falls back.
Protected PostgreSQL initialization validates only; container rebind/start is
blocked pending separate cutover approval. Existing backend mode environments,
authentication, roles and seed semantics are retained.

Web tests require an explicit isolated C04 context or labelled synthetic fixture;
they no longer discover developer credentials. This is test plumbing, not a
runtime Web/API change. Existing C04 harness configuration remains supported.

CLI exposes only paths, validated test-record path and no-write migration dry-run.
There is **no real init/migrate/activate/retire command**. Generation provisioning
is synthetic-only. Future migration must separately implement and review the
real creation/transfer transaction; no helper invocation on checkout/startup
automatically creates a project store. No CI/dependency/image change.

## Compatibility, recovery and remaining approval

Legacy foundation, smoke and isolated-verification consumers remain unchanged in
Phase 1; this is not completed migration of the entire consumer graph. They must
be reconciled before retiring their sources. Historical ADR-0005/0007 behavior
and retained evidence are not silently reinterpreted or recursively re-ACL'd.

### Corrective metadata transaction contract — PCS-01..04

Public metadata names reject control/transaction namespaces, credential filenames,
Windows device/stream/traversal names, trailing dots/spaces and case-insensitive
aliases. Ordinary names including active.json remain supported. Internal callers
alone access operation.lock, transactions and pending/backup names.

A live writer keeps an exclusive OS handle on operation.lock. Ordinary writers
never adopt an existing lock. Only explicit reconciliation may reopen a stale
lock, and an exclusive live-process claim must succeed first. Metadata create,
update and rollback stop for any unresolved or unreadable journal. A lock file
may be removed after a failed operation; the unresolved journal remains the
mutation barrier. Session credentials have their separate exclusive lifecycle.

transactions/<UUID>/intent.json is immutable, flushed and read back before
ReplaceFileW. It binds store/target/temporary/backup identities, before/after
digests, transaction ID, global integer revision and prior target revision. No
metadata payload or credential value is stored in the journal. Immutable events
record COMMITTED, ROLLED_BACK, RECOVERY_REQUIRED or explicit reconciliation.
Replaying records reconstructs revisions after process restart. ABORTED intents
consume their global revision; they never become a target's committed revision.
Initial exclusive metadata creation is revision0. SHA-256 is not a revision.

Rollback requires the exact stored receipt, latest target revision, exact current
and backup identities and hashes under the lock. It creates a NEW revision and
preserves the original backup. A->B->C->B cannot revive the old B receipt.

Reconciliation has no CLI command. Explicit accept requires exact after target,
exact before backup and absent temporary; abort requires unchanged before target,
exact pending object and absent backup. A missing target, incomplete/corrupt
intent/event or any other unproven outcome stays blocked for owner-approved
recovery. There is no automatic restore, evidence deletion or stale-lock reset.
Prior fully resolved transactions cannot reconcile over a newer target revision.

### PCS-RR-01 — strict filesystem presence

Independent re-review resolved PCS-01/02 but kept PCS-03/04 PARTIAL because a
Boolean lexists false could conceal access/sharing/I/O failure. Security decisions
now use Presence(PRESENT/ABSENT/UNKNOWN, sanitized category, numeric code).

On Windows OPEN_EXISTING requests metadata rights only and OPEN_REPARSE_POINT.
Opened ancestors remain held without delete sharing while their identities,
directory types, owners and ACE policies are verified. Only native not-found2/3
under an already verified parent can establish ABSENT; a missing/unverifiable
root, unsafe ancestor, redirect/hardlink or failed query is UNKNOWN. A first
missing component can prove descendant absence only after the preceding chain
has been verified. Final PRESENT proves an observed object, not permission to
adopt/use it; existing-directory policy and object-class validation still apply.

Access denied5, sharing32/33, device/I/O failures and other query errors retain
sanitized categories/codes. Metadata verification failures remain UNKNOWN;
unavailable security evidence is not mislabeled as a known unsafe-owner audit
exception. Raw messages/paths/contents are not copied into diagnostics.
The POSIX implementation remains a synthetic fixture shim, not Linux production.

verified_absent raises for UNKNOWN. Native backup guard, replace poststate,
reconcile accept/abort, object matching, intent presence and stale-lock lookup use
this boundary. UNKNOWN before replace prevents the native call; UNKNOWN after
replace or during reconciliation preserves the unresolved journal and artifacts,
without a terminal/resolution record or automatic recovery. Existing writer and
receipt/revision controls remain authoritative.

Dry-run exposes targetPresence/targetParentPresence/sourcePresence independently
from namespaceSafety. targetCollision is true for observed existing target/parent,
false only if both are verified ABSENT, otherwise UNKNOWN. Every UNKNOWN adds a
blocker. An explicitly allowlisted legacy metadata audit may collect accessible
owner/ACL information in a known unsafe namespace, but never upgrades its UNKNOWN
presence or eligibility; query-error UNKNOWN skips downstream target inspection.
This is no authority to read credentials, retire legacy copies or migrate.

The subsequent publication preflight exposed one Linux negative-fixture failure:
unsafe owner injection reached _presence_stat but not pin's direct lstat.
A test-local Path adapter now presents the same synthetic metadata to the real
pin policy on the selected backend instance. No global filesystem patch or
production policy change. The original UNSAFE assertion remains, with safe,
existing-object, missing-object and scope-restoration controls. Windows72/0/0
and Ubuntu59 PASS/0 FAIL/13 Windows-only skips were rerun successfully.
Linux execution covers the portable fixture only, not production protected mode.

Presence results are snapshots. Once handles close, later pathname operations
retain the documented TOCTOU limitations; this does not add a namespace lease,
same-user isolation, power-loss proof or real migration approval. The final
security review cleared the five Phase 1 findings for PR preparation; subsequent
owner PR review and hosted backend checks remain separate gates.

FlushFileBuffers/fsync on files and real process-exit tests establish bounded
process-restart recovery on the observed filesystem. They do NOT establish
directory-entry persistence, disk/controller power-loss ordering or complete
crash consistency. Real migration readiness remains BLOCKED for those guarantees.
The POSIX backend is synthetic-fixture-only; its two renames are not evidence of
Windows atomicity or Linux production support. No new hosted/Linux CI ran.

Read-only metadata consumers do not hold the mutation lock across their later
pathname opens. Replace and cleanup also retain pathname operations; no end-to-end
TOCTOU freedom or same-account adversary isolation is claimed. Keep uncertain
transaction evidence until separately reviewed recovery, even when synthetic
fixtures are normally eligible for their explicitly owned test teardown.

Root `.demo/settings.json` remains owned by CodexSandboxOffline and exposed under
legacy ACLs. Its separately approved owner action is required even if a new copy
works. No developer deletion/recreation workaround, automatic rotation or
restoration of an insecure legacy copy as rollback. Developer PostgreSQL
recreation/rebinding and real credential transfer/retirement need new approval.

AC-001-04/05 remain unaccepted; Feature001/D1-08/Day1 PARTIAL. Original raw-cache
scanner FAIL, bounded attribution and privacy retention review2026-10-25 remain
documented, with no new suppression or blanket publication assurance.
