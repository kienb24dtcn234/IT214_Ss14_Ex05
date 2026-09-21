package com.storex.combo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Đối tác 2: Đặt phòng khách sạn (API riêng).
// Mô phỏng lỗi/nghẽn theo scenario để demo.
@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    public String reserve(String orderId, String scenario) {
        // Giả lập đối tác phản hồi chậm -> gây timeout ở Orchestrator.
        if ("HOTEL_TIMEOUT".equals(scenario)) {
            log.info("🏨 [Hotel] Đối tác phản hồi chậm (giả lập 5s)...");
            try {
                Thread.sleep(5000);   // lâu hơn timeout 2s của Orchestrator
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Giả lập hết phòng.
        if ("HOTEL_FAIL".equals(scenario)) {
            throw new IllegalStateException("Khách sạn đã hết phòng");
        }
        String booking = "HOTEL-" + orderId;
        log.info("🏨 [Hotel] Đặt phòng thành công: {}", booking);
        return booking;
    }

    // COMPENSATE: hủy phòng đã đặt.
    public void cancel(String orderId) {
        log.warn("↩️  [Hotel COMPENSATE] Hủy phòng khách sạn của đơn {}", orderId);
    }
}
