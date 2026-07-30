package com.example.monitoring.alert.entity;

/**
 * Alert lifecycle statuses used across entity, service, and API layers.
 */
public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    CLOSED,
    DISMISSED
}

