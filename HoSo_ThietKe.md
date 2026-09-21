# BÀI TẬP 5 — HỆ THỐNG ĐẶT COMBO "CHUYẾN ĐI TRỌN GÓI" (SAGA)

**Bối cảnh:** Khách đặt combo **Vé máy bay + Phòng khách sạn** trong một giao dịch. Yêu cầu: *hoặc thành công cả hai, hoặc hủy toàn bộ*. Nhưng hai hệ thống thuộc **hai đối tác khác nhau, API khác nhau, không có transaction DB chung** → không thể dùng ACID → phải dùng **Saga + Compensating Transaction**.

---

## a) PHÂN TÍCH VẤN ĐỀ

**Các dịch vụ tham gia:** Order Combo (điều phối), Flight Service (đối tác 1), Hotel Service (đối tác 2), Payment Service.

**Các bước trong luồng:**
1. Đặt vé máy bay (Flight).
2. Đặt phòng khách sạn (Hotel).
3. Thanh toán (Payment).
4. Xác nhận combo.

**Các điểm có thể thất bại:**
- Flight đặt được nhưng Hotel hết phòng.
- Cả hai đặt được nhưng Payment thất bại (thẻ bị từ chối).
- Đối tác Hotel **phản hồi chậm/timeout** (không rõ đã đặt hay chưa).
- Lỗi mạng giữa các bước.

---

## b) CHỌN MÔ HÌNH SAGA: **ORCHESTRATION**

Lý do:
- Ràng buộc "**all or nothing**" cần một nơi kiểm soát toàn cục để quyết định commit hay rollback toàn bộ — Orchestration làm việc này rõ ràng.
- Hai đối tác **API khác nhau, đồng bộ (REST)** — Orchestrator gọi tuần tự và bắt lỗi từng bước dễ hơn là rải sự kiện.
- Cần **xử lý timeout/retry/compensate** phức tạp → tập trung ở Orchestrator dễ kiểm soát & debug hơn Choreography (vốn dễ thành "event soup").

---

## c) KIẾN TRÚC & CƠ CHẾ BÙ TRỪ

```
                    ┌───────────────────────┐
                    │   ComboOrchestrator    │  (bộ điều phối)
                    └───────────┬───────────┘
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                        ▼
  ┌───────────┐          ┌───────────┐            ┌───────────┐
  │  Flight   │          │   Hotel   │            │  Payment  │
  │  Service  │          │  Service  │            │  Service  │
  └───────────┘          └───────────┘            └───────────┘

LUỒNG THUẬN:  Flight.reserve -> Hotel.reserve -> Payment.charge -> CONFIRM

BÙ TRỪ (khi lỗi, chạy NGƯỢC LẠI, chỉ hoàn tác bước đã thành công):
   Payment.refund  ->  Hotel.cancel  ->  Flight.cancel
```

| Bước | Hành động | Bù trừ (Compensation) |
|------|-----------|-----------------------|
| Flight | `reserve` | `cancel` (hủy vé) |
| Hotel | `reserve` | `cancel` (hủy phòng) |
| Payment | `charge` | `refund` (hoàn tiền) |

---

## d) XỬ LÝ ĐỐI TÁC PHẢN HỒI CHẬM

- **Timeout:** mỗi lần gọi Hotel bọc timeout **2 giây** (dùng `Future.get(2s)`). Quá hạn → coi như thất bại tạm thời.
- **Retry:** thử lại tối đa **2 lần** khi timeout (đối tác chỉ chậm nhất thời).
- **Compensate:** hết retry vẫn timeout → ném lỗi → Orchestrator hủy vé máy bay đã đặt (rollback).
- **Idempotency (khuyến nghị thực tế):** khi retry Hotel, cần gửi kèm khóa idempotent để tránh đặt trùng 2 phòng nếu request đầu thực ra đã tới đối tác.

---

## e) CÁC KỊCH BẢN DEMO (xem README)
1. **Thành công cả hai** → CONFIRMED.
2. **Hotel thất bại** (hết phòng) → rollback Flight.
3. **Payment thất bại** → rollback Hotel + Flight.
4. **Timeout khi gọi Hotel** → retry 2 lần → vẫn timeout → rollback Flight.
