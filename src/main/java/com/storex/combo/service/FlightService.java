package com.storex.combo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Đối tác 1: Đặt vé máy bay (API riêng).
@Service
public class FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightService.class);

    public String reserve(String orderId) {
        String booking = "FLIGHT-" + orderId;
        log.info("✈️  [Flight] Đặt vé thành công: {}", booking);
        return booking;
    }

    // COMPENSATE: hủy vé đã đặt.
    public void cancel(String orderId) {
        log.warn("↩️  [Flight COMPENSATE] Hủy vé máy bay của đơn {}", orderId);
    }
}
