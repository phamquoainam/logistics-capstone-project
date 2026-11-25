# P0-NOTICE-BIND-01 — Merchant-owned SQL event predicate for the notice guard

Status: **OWNER APPROVED — isolated SOURCE/TEST only; implementation not started**.
Owner approval: P0-NOTICE-BIND-01,2026-11-24. No runtime/PUBLIC EXECUTE, cross-owner
table SELECT, mutation, deployment or real grant is authorized. V43 implementation
starts only after the relevant V42 security gates PASS.
Parent manifest remains byte-identical at SHA-256
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
This does not reopen approved business notices, activation, policies or P0 amendments.

## Exact unresolved boundary

P0-PORT-07 already approves Notifications enqueue through a genuine current
Identity callback and Merchant public event port. P0-DB-15 also requires direct
DML without a matching committed-command/event binding to be denied, with no
Notifications SQL into Merchant-owned tables. The exact SQL manifest lists five
functions; none provides this event-binding predicate. No corresponding function
or reusable Merchant notice guard currently exists in source.

A Java port cannot be called by a PostgreSQL BEFORE INSERT guard. An arbitrary
custom GUC is not authority. Giving Notifications raw Merchant-table SELECT or
letting runtime insert an unverified outbox row would weaken the approved boundary.
No such workaround is implemented. The notice writer/activation dependency remains
fail-closed until the approved narrow interface and its security gates are implemented; independent V42 work proceeds.

## Exact addition [PROPOSED]

```sql
merchant.application_notice_bound(
    UUID event_id, UUID application_id, UUID applicant_id, TEXT template
) RETURNS BOOLEAN
```

- Merchant owns the implementation and all Merchant-table reads. Returns a boolean
  only, never an email, legal field, proof, recipient envelope or protected payload.
- Finite mappings only: REQUEST_INFORMATION → REQUEST_INFORMATION; REJECTED →
  REJECT; APPROVED → APPROVE; WITHDRAWN → WITHDRAW. No arbitrary event selector.
- Require exact application/event/applicant/target/policy and canonical receipt
  binding, matching actor/realm/operation and applied version. The originating
  command event and notice INSERT must belong to the same database transaction.
- APPROVED additionally requires the complete atomic V43 activation binding;
  no Organization grant, financial state, Carrier authority or delivery claim.
- Fixed search_path; fully qualified non-PG references. No PUBLIC or runtime
  EXECUTE; only the already approved NOLOGIN `merchant_governance_writer` used by
  the private notification guard may invoke it. No new role or role membership.
- The existing notification-owned trigger invokes only this predicate. It does
  not read Merchant private tables. Runtime remains unable to call private trigger
  functions or bypass the notification row lifecycle guards.
- P0-PORT-07 still independently revalidates the recipient against the exact
  current Identity callback subject before encryption/enqueue. This predicate is
  structural command/event evidence, never replacement authentication.

## Preservation / scope

Add this function/ACL only in the new V43 stream. Do not edit V1–V42 or approved
manifest bytes. No new HTTP API, DTO, table, column, permission, feature flag,
provider, real publication, runtime activation or deployment. Existing notice
table/worker/atomic enqueue requirements remain unchanged.

## Required acceptance after approval

Isolated PostgreSQL18: exact positive same-transaction binding; rollback leaves no
notice/event/activation; wrong event/application/applicant/template, old event,
foreign actor/realm, missing receipt and direct runtime INSERT denied; no PUBLIC/
runtime EXECUTE; no notification-side Merchant table SELECT; concurrent enqueue
unique(event,template); current recipient substitution denial; provider acceptance
is not delivery. No real mail or database demo/UAT mutation.
