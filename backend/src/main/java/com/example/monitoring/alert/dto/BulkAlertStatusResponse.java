package com.example.monitoring.alert.dto;

import com.example.monitoring.alert.entity.AlertStatus;

import java.util.List;

public class BulkAlertStatusResponse {

    private final int requestedCount;
    private final int successCount;
    private final int failureCount;
    private final List<ItemResult> results;

    public BulkAlertStatusResponse(int requestedCount,
                                   int successCount,
                                   int failureCount,
                                   List<ItemResult> results) {
        this.requestedCount = requestedCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = List.copyOf(results);
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public List<ItemResult> getResults() {
        return results;
    }

    public record ItemResult(
            Long id,
            boolean success,
            AlertStatus status,
            String error) {
    }
}
