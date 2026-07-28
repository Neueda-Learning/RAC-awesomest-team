package com.example.monitoring.transaction.dto;

import java.time.LocalDateTime;

public class GenerateTransactionsResponse {

    private int generatedCount;
    private LocalDateTime generatedAt;

    public GenerateTransactionsResponse() {
    }

    public GenerateTransactionsResponse(int generatedCount, LocalDateTime generatedAt) {
        this.generatedCount = generatedCount;
        this.generatedAt = generatedAt;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}

