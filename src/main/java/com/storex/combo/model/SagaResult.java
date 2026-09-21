package com.storex.combo.model;

import java.util.ArrayList;
import java.util.List;

public class SagaResult {
    private boolean success;
    private String message;
    private final List<String> steps = new ArrayList<>();

    public void log(String step) { steps.add(step); }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getSteps() { return steps; }
}
