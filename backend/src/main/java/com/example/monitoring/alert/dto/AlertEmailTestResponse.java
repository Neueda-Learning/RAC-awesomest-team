package com.example.monitoring.alert.dto;

import java.time.LocalDateTime;

public record AlertEmailTestResponse(
        boolean sent,
        String recipient,
        LocalDateTime sentAt,
        String message
) {
}
