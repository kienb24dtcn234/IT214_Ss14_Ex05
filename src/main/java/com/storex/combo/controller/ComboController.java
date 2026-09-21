package com.storex.combo.controller;

import com.storex.combo.model.ComboRequest;
import com.storex.combo.model.SagaResult;
import com.storex.combo.orchestrator.ComboOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/combo")
public class ComboController {

    private final ComboOrchestrator orchestrator;

    public ComboController(ComboOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    // Đặt combo. Body chứa scenario để demo các kịch bản lỗi.
    @PostMapping("/book")
    public SagaResult book(@RequestBody ComboRequest request) {
        return orchestrator.book(request);
    }
}
