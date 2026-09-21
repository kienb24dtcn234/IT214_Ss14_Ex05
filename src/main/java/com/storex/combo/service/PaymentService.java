package com.storex.combo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Thanh toán combo.
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public String charge(String orderId, double amount, String scenario) {
        if ("PAYMENT_FAIL".equals(scenario)) {
            throw new IllegalStateException("Thanh toán thất bại (thẻ bị từ chối)");
        }
        log.info("💳 [Payment] Trừ tiền {} cho combo {} thành công", amount, orderId);
        return "PAID-" + orderId;
    }

    // COMPENSATE: hoàn tiền.
    public void refund(String orderId, double amount) {
        log.warn("↩️  [Payment COMPENSATE] Hoàn tiền {} cho combo {}", amount, orderId);
    }
}
