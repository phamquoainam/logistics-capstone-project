# Milestone B — Local/Test Station operations

Use the existing local HTTPS launcher, PostgreSQL18 and verified personal Identity.
No init, credential/certificate rotation, container replacement or fixture seed.
See [contract/tasks](../implementation/milestone-b-station-acceptance.md) and
[accepted unpaid intake exception](../decisions/br13-local-intake-01.md).

## Operator assignment, explicit and bounded

Migration V21 creates only the three approved SIM-DROP records. **No Staff account
or assignment is seeded in the original database.** Test Staff fixtures exist only
inside owned disposable test containers.

Owner/operator selects an existing verified personal account intended to act as
Station Staff. Obtain its exact UUID from that account's real customer-auth/me
response after login; do not send credentials/tokens/PII into chat. Identity remains
the authentication owner. A Dispatch assignment is an additional permission, not a
replacement account or a SYSTEM/admin privilege. Wrong/nonexistent/unverified
Identity cannot pass runtime admission merely because an assignment row exists.

From D:\TLCN\e-logistic, explicitly grant only the chosen Station:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\station-staff.ps1 -AccountId '<VERIFIED_ACCOUNT_UUID>' -Station SIM-DROP-A1 -Action GRANT
```

The process-scoped execution flag follows local.cmd; it changes no Windows policy.
The script reads existing PostgreSQL migrator configuration only; never Gmail/AWS
secrets. It verifies the existing container/project/health and calls the owner-only
Dispatch provisioning function in a LOCAL transaction. Runtime has no EXECUTE or
assignment-write permission. Grant/revoke is audited with the authenticated database
operator principal, target, Station, version and server time. A repeated unchanged
grant has no additional authority/audit effect. Revoke with the same command and
-Action REVOKE. Capability/revocation changes participate in the intake admission lock.
No account is created and no data or migration history is reset. Do not automatically
run this for the Owner account or any account without explicit operator disposition.

## Actual workflow

1. Customer logs in at https://127.0.0.1:5173/login and books an eligible simulated
   ROAD_STANDARD draft. Open own Shipment → Chứng từ or Kiện hàng → Tải / In nhãn.
   Save PDF or open the prepared PDF print view; use100x150mm at100% scale.
2. Each Parcel has its own stable AP-P code and Code128. The barcode contains no
   address, credential or payment proof. The PDF is private and not a public asset.
3. Assigned Staff opens https://127.0.0.1:5173/operations/station. The existing login
   returns to this separate Operations screen. A personal-only user is denied.
4. Choose an actually assigned SIM-DROP and scan each Parcel with a keyboard-wedge
   scanner or enter its full code. Backend resolves the booked source and records
   the actual scan. A scan is not yet custody or acceptance.
5. Check all Parcel identifiers, count, goods, packaging and measurements against
   the saved facts. Missing/foreign/duplicate scans or any discrepancy block intake;
   do not edit weight/price here. No partial-acceptance or invented hold is supported.
6. After every Parcel is scanned and matched, explicitly confirm full intake.
   Backend atomically stores acceptance, custody, tracking, Parcel events, audit and
   receipt. UNPAID remains unchanged; no Payment Success/receipt/COD is generated.
7. On an unknown network outcome, use Kiểm tra kết quả đã lưu. It reads the original
   scoped receipt/source; it never resends the acceptance command automatically.
8. Customer refreshes own Shipment detail/History/Timeline to see the actual committed
   Station event. AT_STATION is not In Transit/Delivered. Pickup/collection/delivery,
   Production/Merchant and further operational legs remain unavailable.

Revoked Identity/Station permission denies future reads, downloads, scan, confirm
and receipt recovery. Customer-visible events contain Station/time/provenance;
internal Staff IDs and operational audit are not exposed through Personal tracking.
