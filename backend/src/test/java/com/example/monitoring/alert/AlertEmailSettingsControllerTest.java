package com.example.monitoring.alert;

import com.example.monitoring.alert.controller.AlertEmailSettingsController;
import com.example.monitoring.alert.dto.AlertEmailSettingsRequest;
import com.example.monitoring.alert.dto.AlertEmailSettingsResponse;
import com.example.monitoring.alert.dto.AlertEmailTestResponse;
import com.example.monitoring.alert.notification.AlertEmailSettingsService;
import com.example.monitoring.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertEmailSettingsControllerTest {

    @Mock
    private AlertEmailSettingsService settingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AlertEmailSettingsController(settingsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSettings_shouldExposePasswordStateButNeverPasswordValue() throws Exception {
        when(settingsService.getSettings()).thenReturn(response());

        mockMvc.perform(get("/admin/email-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.smtpHost").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.passwordConfigured").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.smtpPassword").doesNotExist());
    }

    @Test
    void updateSettings_shouldValidateAndReturnSavedSettings() throws Exception {
        when(settingsService.updateSettings(any(AlertEmailSettingsRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/admin/email-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "fromAddress": "sender@gmail.com",
                                  "toAddress": "recipient@example.com",
                                  "smtpHost": "smtp.gmail.com",
                                  "smtpPort": 587,
                                  "smtpUsername": "sender@gmail.com",
                                  "smtpAuth": true,
                                  "starttlsEnabled": true,
                                  "starttlsRequired": true,
                                  "maxAttempts": 3,
                                  "retryDelayMs": 60000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persisted").value(true));
    }

    @Test
    void updateSettings_shouldRejectInvalidEmailAndPort() throws Exception {
        mockMvc.perform(put("/admin/email-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "fromAddress": "not-an-email",
                                  "toAddress": "recipient@example.com",
                                  "smtpHost": "smtp.example.com",
                                  "smtpPort": 70000,
                                  "smtpUsername": "sender@example.com",
                                  "smtpAuth": true,
                                  "starttlsEnabled": true,
                                  "starttlsRequired": true,
                                  "maxAttempts": 3,
                                  "retryDelayMs": 60000
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendTestEmail_shouldReturnRecipientAndResult() throws Exception {
        when(settingsService.sendTestEmail()).thenReturn(new AlertEmailTestResponse(
                true,
                "recipient@example.com",
                LocalDateTime.of(2026, 7, 30, 14, 0),
                "Test email sent successfully"));

        mockMvc.perform(post("/admin/email-settings/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.recipient").value("recipient@example.com"));
    }

    private AlertEmailSettingsResponse response() {
        return new AlertEmailSettingsResponse(
                true,
                "sender@gmail.com",
                "recipient@example.com",
                "smtp.gmail.com",
                587,
                "sender@gmail.com",
                true,
                true,
                true,
                3,
                60_000L,
                true,
                true,
                LocalDateTime.of(2026, 7, 30, 13, 0));
    }
}
