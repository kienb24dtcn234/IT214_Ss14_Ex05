package com.storex.combo.orchestrator;

import com.storex.combo.model.ComboRequest;
import com.storex.combo.model.SagaResult;
import com.storex.combo.service.FlightService;
import com.storex.combo.service.HotelService;
import com.storex.combo.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * ORCHESTRATOR đặt combo Vé máy bay + Khách sạn.
 * Luồng: Flight -> Hotel -> Payment -> Confirm.
 * "All or nothing": bất kỳ bước nào lỗi -> compensate theo thứ tự ngược lại.
 * Riêng Hotel (đối tác hay chậm) được bọc TIMEOUT 2s + RETRY tối đa 2 lần.
 */
@Component
public class ComboOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ComboOrchestrator.class);

    private static final long HOTEL_TIMEOUT_SEC = 2;
    private static final int HOTEL_MAX_ATTEMPTS = 2;

    private final FlightService flightService;
    private final HotelService hotelService;
    private final PaymentService paymentService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ComboOrchestrator(FlightService flightService,
                             HotelService hotelService,
                             PaymentService paymentService) {
        this.flightService = flightService;
        this.hotelService = hotelService;
        this.paymentService = paymentService;
    }

    public SagaResult book(ComboRequest req) {
        SagaResult result = new SagaResult();
        String orderId = req.getOrderId();
        String scenario = req.getScenario();

        boolean flightBooked = false;
        boolean hotelBooked = false;
        boolean paid = false;

        try {
            // B1: Đặt vé máy bay
            flightService.reserve(orderId);
            flightBooked = true;
            result.log("Flight.reserve -> OK");

            // B2: Đặt phòng khách sạn (TIMEOUT 2s + RETRY 2 lần)
            reserveHotelWithTimeout(orderId, scenario);
            hotelBooked = true;
            result.log("Hotel.reserve -> OK");

            // B3: Thanh toán
            paymentService.charge(orderId, req.getAmount(), scenario);
            paid = true;
            result.log("Payment.charge -> OK");

            // B4: Hoàn tất
            result.setSuccess(true);
            result.setMessage("Đặt combo thành công (cả vé và phòng)");
            result.log("Combo CONFIRMED");
            return result;

        } catch (Exception ex) {
            log.error("❌ Saga combo thất bại: {}. Bắt đầu COMPENSATE...", ex.getMessage());
            result.setSuccess(false);
            result.setMessage("Đặt combo thất bại: " + ex.getMessage());
            result.log("LỖI: " + ex.getMessage());

            // COMPENSATE theo thứ tự ngược lại, chỉ hoàn tác bước đã thành công.
            if (paid) {
                paymentService.refund(orderId, req.getAmount());
                result.log("COMPENSATE Payment.refund");
            }
            if (hotelBooked) {
                hotelService.cancel(orderId);
                result.log("COMPENSATE Hotel.cancel");
            }
            if (flightBooked) {
                flightService.cancel(orderId);
                result.log("COMPENSATE Flight.cancel");
            }
            return result;
        }
    }

    // Gọi Hotel với timeout + retry. Hết retry vẫn timeout/lỗi -> ném ra để saga compensate.
    private void reserveHotelWithTimeout(String orderId, String scenario) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= HOTEL_MAX_ATTEMPTS; attempt++) {
            Future<String> future = executor.submit(() -> hotelService.reserve(orderId, scenario));
            try {
                future.get(HOTEL_TIMEOUT_SEC, TimeUnit.SECONDS);
                return;   // thành công
            } catch (TimeoutException te) {
                future.cancel(true);
                last = new TimeoutException("Hotel timeout (lần " + attempt + ")");
                log.warn(">> Hotel timeout lần {} -> thử lại...", attempt);
            } catch (ExecutionException ee) {
                // Lỗi nghiệp vụ (hết phòng) -> không retry, ném ngay.
                throw (Exception) ee.getCause();
            }
        }
        throw last;   // hết retry vẫn timeout
    }
}
