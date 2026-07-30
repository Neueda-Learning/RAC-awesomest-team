package com.example.monitoring.alert.controller;

import com.example.monitoring.alert.dto.AlertEmailSettingsRequest;
import com.example.monitoring.alert.dto.AlertEmailSettingsResponse;
import com.example.monitoring.alert.dto.AlertEmailTestResponse;
import com.example.monitoring.alert.notification.AlertEmailSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/email-settings")
public class AlertEmailSettingsController {

    private final AlertEmailSettingsService settingsService;

    public AlertEmailSettingsController(AlertEmailSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<AlertEmailSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<AlertEmailSettingsResponse> updateSettings(
            @Valid @RequestBody AlertEmailSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }

    @PostMapping("/test")
    public ResponseEntity<AlertEmailTestResponse> sendTestEmail() {
        return ResponseEntity.ok(settingsService.sendTestEmail());
    }
}
