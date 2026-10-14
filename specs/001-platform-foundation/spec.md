# Feature 001: Nền tảng repository, deploy và kiến trúc

**D1-05 evidence update (2026-10-14):** D1-04 approved local scope accepted and
merged in develop/2c609c4. Executable backend CI and local fresh Linux tests PASS:
architecture 23, H2 40/eight PG-only skip, fresh PostgreSQL 48/no skip.
**PARTIAL / READY FOR REMOTE VERIFICATION**: REQ-001-03 actual GitHub CI and
required checks remain unverified. Full original REQ/AC below are unchanged;
six unimplemented modules are not counted as verified. [Evidence](../../docs/implementation/d1-05-ci-evidence.md),
[traceability](../../docs/quality/traceability.md#d1-05--ci-gate-2026-10-14).

**Status:** DRAFT_SPEC / PARTIAL — approved D1-03 PostgreSQL/Demo-001 database milestone verified; full feature gates remain open ([evidence](../../docs/implementation/d1-03-postgresql-evidence.md)).
**Owners / Bounded Context:** `platform, workflow; all module skeletons`  
**Actors:** Developer, CI  

**D1-04 local evidence (2026-10-14):** real module boundaries/direction/cycles,
negative fixture effectiveness and unchanged Demo-001 regressions PASS.
[Evidence](../../docs/implementation/d1-04-architecture-evidence.md).
REQ-001-03/AC-001-03 remain PARTIAL because actual CI execution is unverified;
full feature/acceptance criteria below are unchanged.
**Priority:** P0 nền tảng  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Nền tảng repository, deploy và kiến trúc là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Developer, CI**, tôi muốn thực hiện nghiệp vụ **Nền tảng repository, deploy và kiến trúc** để đạt: **Một developer mới có thể chạy `docker compose up` cùng hướng dẫn, migrate và nhận health response; Maven/web/mobile compile theo target đã công bố; architecture test xanh.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Developer clone repo → cấu hình an toàn → chạy Compose → migration → seed tối thiểu → health/readiness → kiến trúc-test → smoke build.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-001-01 | Khởi tạo một Maven/Spring Boot backend, một web app React/TypeScript, một React Native/TypeScript app theo build phù hợp; skeleton không sinh 9 nghiệp vụ giả. | NFR/RQ |
| REQ-001-02 | Flyway migrate PostgreSQL từ DB trống; phân schema ownership; runtime không có DDL. | NFR/RQ |
| REQ-001-03 | Module verification fail khi package gọi `othermodule.internal`; architecture test được chạy trong CI. | NFR/RQ |
| REQ-001-04 | OpenAPI stub validate và generated TS client đúng hợp đồng; DB/Redis/RabbitMQ checks rõ trạng thái. | NFR/RQ |
| REQ-001-05 | Không commit secrets hoặc baseline version chưa xác nhận. | NFR/RQ |


### Mapping FR gốc
Không có FR riêng: đây là enabler cho các NFR và RQ trong báo cáo.

### Business invariants / NFR cần giữ
- **BR:** Áp dụng nguyên tắc correctness, audit, ownership trong docs/principles.md.
- **NFR:** NFR-O01, NFR-O02, NFR-O03, NFR-M01, NFR-M02.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-001-01: Migrate DB lần thứ hai không phá dữ liệu
- EC-001-02: Compose restart vẫn giữ dữ liệu volume
- EC-001-03: Module package vi phạm bị kiểm thử chặn
- EC-001-04: Build clean trên máy khác
- EC-001-05: Artifact đóng gói không lộ secret

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-001-01 — cold start:** Given máy dev có Docker/JDK/Node baseline đã chốt, When setup từ clean clone theo runbook, Then PostgreSQL/Redis/RabbitMQ lên, migration chạy, backend health/readiness đúng, web build được và native mobile spike có log kết quả.

**AC-001-02 — idempotent migration:** Given DB đã có schema P0 tối thiểu, When chạy lại migration/Compose restart, Then không drop dữ liệu đã commit hoặc tạo migration drift.

**AC-001-03 — module boundary:** Given một lớp test cố tình import `order.internal` từ `finance`, When chạy architecture verification, Then CI fail có thông báo phụ thuộc bị cấm; xóa vi phạm thì pass.

**AC-001-04 — security:** Given repository có `.env.example`, When chạy secret/config audit, Then không có credential thật và backend fail-fast nếu biến bắt buộc chưa được cấu hình.

**AC-001-05 — reproducibility:** Given một developer khác, When thực hiện runbook trên môi trường được công bố, Then tạo được kết quả smoke test cùng trạng thái và có log công bố command/version.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** Không có feature bắt buộc trước.
- **Ngoài phạm vi:** Không triển khai business logic tạo đơn/giữ hàng/nạp tiền ở feature này.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Khóa phiên bản Spring/Modulith/Node/Expo, auth approach, provider storage/map; phụ thuộc nền tảng nào chưa chốt cần spike và ADR.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
