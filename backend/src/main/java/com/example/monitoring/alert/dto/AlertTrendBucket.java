package com.example.monitoring.alert.dto;

import java.time.LocalDate;

public class AlertTrendBucket {

    private final LocalDate date;
    private final long count;

    public AlertTrendBucket(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getCount() {
        return count;
    }
}
