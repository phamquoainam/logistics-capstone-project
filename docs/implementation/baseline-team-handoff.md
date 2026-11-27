# Portable baseline handoff — Nam / Hoàng / Triết

Packet status: prepared; not sent and not independently ACKed. Use the exact
merged SHA from the release report/PR, not a dirty local develop checkout.

1. Clone or fetch that SHA in a clean worktree; follow
   [Compose runbook](../runbooks/portable-compose.md).
2. Run `docker compose up --build -d`; trust only this installation's public CA.
   Open https://localhost:5443 and synthetic inbox http://localhost:8025.
3. Record host/OS, Git SHA, five service health results,48 migrations and startup/
   restart verification. Use `scripts/dev/verify-compose.py` if Python available.
4. Explicitly create synthetic Customer via register → actual local OTP → login.
   Optional browser smoke also verifies Business Contact while retaining DRAFT.
5. ACK that existing personal profiles/credentials/demo volumes were unnecessary.
   Do not claim another member's machine PASS. Fresh volumes contain no internal
   identity/grant; canonical Business/Admin/OPS onboarding still requires authority.

Nam (V1-A) is temporary CODE_WRITER for root Compose, infra/local, baseline
workflow, orchestration/runbook and release integration until baseline handback.
No new domain authority follows from this assignment. Check current PR/branch
before touching shared source; coordinate actual conflicts, never drop WIP.

Hoàng (V1-B) receives [accepted dual-audience S1 evidence](v1-dual-audience-source-test-execution.md),
[BIA contract](v1-business-station-intake.delta.md),
[completeness guard approval](v1-intake-completeness-guards.delta.md) and
[NWS foundation](carrier-network-operational-staff-readiness.md).
Next Inventory → Sorting → Manifest is
[Spec012 PROPOSED/BLOCKED_CONTRACT](spec012-station-preparation-contract.proposed.md).
No SPM, Transfer or Dispatch runtime/permission is delivered by this baseline.

Triết (V1-C) receives the same source revision/S1 snapshot and the preserved
Parcel/custody/tracking/public boundary. Dispatch/Courier/Pickup/POD need their
own contracts; no implicit Admin/Finance/Operations grant is introduced.

Member assignments and calendar under [v1-team](v1-team/) remain original Owner
documents. Older planning S1 PARTIAL is superseded by accepted S1 execution;
calendar/assignment versions are recorded in the release report. No broad S2–S8
implementation or unknown team decision has been inferred.
