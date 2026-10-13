# Exception & Recovery Runbook — P0

| Failure | Detection | Safe action | Must not do |
| --- | --- | --- | --- |
| Payment UNKNOWN | Payment intent no verified outcome | Query Sandbox via approved adapter, retry same reference and dedupe | Manually credit without verification |
| Outbox stuck | High pending age / publish error | Inspect event+attempt, restore broker, replay same event ID | Delete event to suppress error |
| Inbox duplicate | Unique consumer/event hit | Ack duplicate after confirming prior effect | Apply side effect twice |
| Stock conflict | Reservation fail / negative availability guard | Reload stock and reject/retry with backoff | Force inventory count |
| Two active assignments | Unique constraint conflict | Re-read active record and resolve reassign with audit | Delete assignment history |
| Offline task stale | Mobile conflict after reconnect | Inform shipper, fetch task current state, supervisor resolution | Mark delivered locally as server success |
| Reconciliation close race | Unique closed COD item conflict | Re-check eligibility, report overlap, retry new preview | Modify old CLOSED period |
| Maps unavailable | Matrix/geocode error | Return explicit unavailable or authorized manual plan | Invent GPS route or cost |

Manual recovery requires approved role, reason, source, time and audit. Replaying job must still pass BR/state/owner rules.
