# Delivery principles V3 — phương pháp Owner đã chốt

**Spec-first → Spec-by-Spec → Full-stack End-to-end Delivery → Full Spec Acceptance.**

Owner authorization2026-10-27 cho docs rebuild/canonical promotion, không production implementation. Vertical slices chỉ là decomposition nội bộ trong một spec/gói contract phụ thuộc, không methodology thay thế hoặc lý do tuyên bố partial feature thành complete. Phát triển feature trong backend/web hiện hữu, không repo theo role/module.

1. Canonical spec business/actor/guard/state/AC trước design/contracts/code. Accepted architecture/security giữ nguyên cho tới controlled amendment explicit; latest Owner account-first direction là target, chưa tự đổi runtime.
2. Chốt first-use blockers, exact public contracts/data ownership/fixtures. Không hỏi lại policy đã được Owner chốt; technical choices thuộc accepted security/schema/provider gates phải có reviewed packet cụ thể.
3. Mỗi owning spec có đầy đủ BE/API/PG/FE hoặc verified channel/security/test/evidence. Minimum upstream contracts có thể chuẩn bị trước theo dependency, nhưng không coi upstream/parent spec accepted. Không fake catalog/membership/finance/custody để vượt gate.
4. Run relevant unit/architecture/H2/PG18/contract/browser/native when applicable, migration/backward compatibility, negative/concurrency/recovery và mandatory exact CI/report/privacy controls. Không bypass skip hoặc weaken guards. Existing findings giữ đúng scope/status.
5. TASK DONE đúng gate; PR MERGED chỉ integration; SPEC FULL ACCEPTED cần all assigned BC/legacy/supplement AC và Owner record; GROUP_A_B_PORTAL_FULL_ACCEPTANCE cần cross-Spec J-AB-01..10, không chỉ003. Full platform còn C–F/RQ/release gates.
6. Owner material approvals và PR merge/feature acceptance riêng; routine work đã được authorized không tạo extra permission loops. Current docs-only PR dừng Owner review/merge, chưa cho phép coding.

[Roadmap](roadmap-v3.md) · [Canonical index](../../specs/INDEX.md) · [DoR](../codex/definition-of-ready.md) · [Portal coverage](../portal/group-ab-coverage.md) · [Product acceptance](../portal/group-ab-acceptance.md).
