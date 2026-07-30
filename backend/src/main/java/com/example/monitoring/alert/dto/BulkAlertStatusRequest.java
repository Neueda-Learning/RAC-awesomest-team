package com.example.monitoring.alert.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BulkAlertStatusRequest {

    @NotEmpty(message = "ids must not be empty")
    @Size(max = 100, message = "ids must contain at most 100 alerts")
    private List<@NotNull(message = "ids must not contain null values") Long> ids;

    @NotNull(message = "action is required")
    private AlertBulkAction action;

    @Size(max = 1000, message = "notes must not exceed 1000 characters")
    private String notes;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public AlertBulkAction getAction() {
        return action;
    }

    public void setAction(AlertBulkAction action) {
        this.action = action;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
