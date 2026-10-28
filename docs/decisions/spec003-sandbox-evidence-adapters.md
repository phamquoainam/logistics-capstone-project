# Spec003 — sandbox evidence storage and screening

ID: `OWNER-S003-EVIDENCE-SANDBOX-2026-10-28`.

Status: **OWNER_APPROVED_SANDBOX_CONFIGURATION**. Nguồn: phản hồi trực tiếp của Owner ngày 2026-10-28 cho câu hỏi cấu hình E2E upload/scanning: “Kho tệp riêng trên ổ đĩa + ClamAV (khuyến nghị)”. Quyết định bổ sung cấu hình thực thi cho PRIVACY-01 và [contract evidence](../../specs/003-sender-merchant-onboarding/contracts/README.md) phần 63–65; không thay đổi business/security policy đã được duyệt.

- Sandbox lưu evidence trong kho tệp riêng trên ổ đĩa, nằm ngoài public web root; chỉ backend/tiến trình test được sở hữu mới có quyền truy cập trực tiếp.
- ClamAV là scanner thực cho sandbox. Phải có kiểm tra signature/parser/digest, cấu hình và phiên bản engine/signature được ghi nhận; không tạo kết quả CLEAN giả.
- Evidence vào quarantine trước. Scanner lỗi/không sẵn sàng hoặc kết quả chưa xác định không cho reviewer preview/download và không được tính là evidence sạch để submit/approve.
- Giữ giới hạn PDF/PNG/JPEG, 5 MiB/tệp, 5 tệp retained-or-reserved/application; quota/race, quyền hiện hành và tính bất biến của bytes đã submit theo contract.
- Tên/key lưu trữ do server sinh; không nhận filesystem path, storage URL hoặc public URL từ client. Download qua API có scope, no-store/nosniff/attachment và kiểm lại digest.
- Mọi fixture dùng dữ liệu tổng hợp; secrets/config nằm ngoài Git. Resource test phải có ownership và cleanup chính xác. `purgeEnabled=false`; không tự bật destructive purge hay xóa evidence đang được tham chiếu/hold.

Provider production vẫn **chưa được chọn**. Quyết định này không cấp quyền public rollout, thay đổi accepted Identity/Foundation controls hoặc tự merge PR production. Scope còn lại và gate nghiệm thu giữ nguyên trong [tasks](../../specs/003-sender-merchant-onboarding/tasks.md).
