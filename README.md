# SS14 - Bài tập 5: Combo "Chuyến đi trọn gói" (Saga Orchestration)

Đặt combo Vé máy bay + Khách sạn (2 đối tác, API khác nhau, không có DB chung). Dùng **Saga Orchestration** + Compensating Transaction, có timeout/retry cho đối tác chậm.

## Stack
- Spring Boot 3.3.4, Java 17 (demo in-process, chạy được 4 kịch bản)

## Deliverables theo đề
| Yêu cầu | Vị trí |
|---|---|
| a) Phân tích (service, bước, điểm lỗi) | `HoSo_ThietKe.md` |
| b) Chọn mô hình Saga + lý do | `HoSo_ThietKe.md` (Orchestration) |
| c) Sơ đồ kiến trúc + cơ chế compensate | `HoSo_ThietKe.md` |
| d) Xử lý timeout/retry/compensate | `HoSo_ThietKe.md` + `ComboOrchestrator.java` |
| e) Code demo 4 kịch bản | full project |

## Chạy dự án
IntelliJ → Run `ComboApplication` (cổng **8080**), hoặc `./gradlew bootRun`.

## Demo 4 kịch bản (dùng field `scenario`)
### 1. Thành công cả hai
```bash
curl -X POST http://localhost:8080/api/combo/book -H "Content-Type: application/json" \
  -d '{"orderId":"C-1","userId":"U1","amount":500000,"scenario":"SUCCESS"}'
```
→ `success:true`, steps: Flight → Hotel → Payment → CONFIRMED.

### 2. Hotel thất bại → rollback Flight
```bash
curl -X POST http://localhost:8080/api/combo/book -H "Content-Type: application/json" \
  -d '{"orderId":"C-2","userId":"U1","amount":500000,"scenario":"HOTEL_FAIL"}'
```
→ `success:false`, compensate: Flight.cancel.

### 3. Payment thất bại → rollback cả hai
```bash
curl -X POST http://localhost:8080/api/combo/book -H "Content-Type: application/json" \
  -d '{"orderId":"C-3","userId":"U1","amount":500000,"scenario":"PAYMENT_FAIL"}'
```
→ `success:false`, compensate: Hotel.cancel + Flight.cancel.

### 4. Timeout khi gọi Hotel → retry 2 lần → rollback Flight
```bash
curl -X POST http://localhost:8080/api/combo/book -H "Content-Type: application/json" \
  -d '{"orderId":"C-4","userId":"U1","amount":500000,"scenario":"HOTEL_TIMEOUT"}'
```
→ Hotel chậm 5s > timeout 2s, retry 2 lần đều timeout → `success:false`, compensate: Flight.cancel.

## Điểm nhấn thiết kế
- **All-or-nothing** bằng Saga Orchestration (không có ACID chung cho 2 đối tác).
- **Compensation** chạy ngược thứ tự, chỉ hoàn tác bước đã thành công.
- **Timeout 2s + retry 2 lần** cho Hotel (đối tác chậm), dùng `Future.get(timeout)`.
- `SagaResult.steps` = nhật ký tập trung để debug.

## Files chính
- `orchestrator/ComboOrchestrator.java` — điều phối + timeout/retry + compensate
- `service/FlightService.java`, `HotelService.java`, `PaymentService.java`
- `controller/ComboController.java` — `/api/combo/book`
- `HoSo_ThietKe.md` — hồ sơ thiết kế a/b/c/d/e

## Nộp bài
Đẩy thư mục này lên Git repository, nộp link repo + file `HoSo_ThietKe.md`.
