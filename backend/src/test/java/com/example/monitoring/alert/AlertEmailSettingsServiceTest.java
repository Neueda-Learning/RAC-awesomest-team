package com.example.monitoring.alert;

import com.example.monitoring.alert.dto.AlertEmailSettingsRequest;
import com.example.monitoring.alert.dto.AlertEmailSettingsResponse;
import com.example.monitoring.alert.entity.AlertEmailSettings;
import com.example.monitoring.alert.notification.AlertEmailProperties;
import com.example.monitoring.alert.notification.AlertEmailSettingsService;
import com.example.monitoring.alert.notification.AlertEmailSettingsSnapshot;
import com.example.monitoring.alert.notification.AlertSmtpSender;
import com.example.monitoring.alert.repository.AlertEmailSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEmailSettingsServiceTest {

    @Mock
    private AlertEmailSettingsRepository repository;

    @Mock
    private AlertSmtpSender smtpSender;

    private AlertEmailSettingsService service;

    @BeforeEach
    void setUp() {
        AlertEmailProperties defaults = new AlertEmailProperties();
        defaults.setEnabled(false);
        defaults.setFrom("monitoring@example.com");
        defaults.setTo("admin@example.com");
        defaults.setSmtpHost("smtp.example.com");
        defaults.setSmtpPort(587);
        defaults.setSmtpUsername("monitoring@example.com");
        defaults.setSmtpAuth(true);
        defaults.setStarttlsEnabled(true);
        defaults.setStarttlsRequired(true);
        defaults.setMaxAttempts(3);
        defaults.setRetryDelayMs(60_000L);
        service = new AlertEmailSettingsService(repository, defaults, smtpSender);
    }

    @Test
    void getSettings_shouldReturnEnvironmentDefaultsWithoutExposingPassword() {
        when(repository.findCurrent()).thenReturn(Optional.empty());
        when(smtpSender.isPasswordConfigured()).thenReturn(true);

        AlertEmailSettingsResponse response = service.getSettings();

        assertFalse(response.enabled());
        assertEquals("smtp.example.com", response.smtpHost());
        assertEquals(587, response.smtpPort());
        assertTrue(response.passwordConfigured());
        assertFalse(response.persisted());
    }

    @Test
    void updateSettings_shouldPersistOnlyNonSecretValues() {
        when(repository.findCurrent()).thenReturn(Optional.empty());
        when(repository.save(any(AlertEmailSettings.class)))
                .thenAnswer(invocation -> {
                    AlertEmailSettings entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        AlertEmailSettingsResponse response = service.updateSettings(validRequest());

        ArgumentCaptor<AlertEmailSettings> captor =
                ArgumentCaptor.forClass(AlertEmailSettings.class);
        verify(repository).save(captor.capture());
        assertEquals("smtp.gmail.com", captor.getValue().getSmtpHost());
        assertEquals("sender@gmail.com", captor.getValue().getSmtpUsername());
        assertTrue(response.persisted());
    }

    @Test
    void updateSettings_shouldRejectRequiredTlsWhenTlsIsDisabled() {
        AlertEmailSettingsRequest invalid = new AlertEmailSettingsRequest(
                true,
                "sender@gmail.com",
                "recipient@example.com",
                "smtp.gmail.com",
                587,
                "sender@gmail.com",
                true,
                false,
                true,
                3,
                60_000L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateSettings(invalid));

        assertTrue(exception.getMessage().contains("starttlsEnabled"));
    }

    @Test
    void sendTestEmail_shouldUseEffectiveSettingsAndEnvironmentPasswordState() {
        AlertEmailSettings entity = persistedSettings();
        when(repository.findCurrent()).thenReturn(Optional.of(entity));
        when(smtpSender.isPasswordConfigured()).thenReturn(true);

        service.sendTestEmail();

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtpSender).send(
                any(AlertEmailSettingsSnapshot.class),
                messageCaptor.capture());
        assertEquals("sender@gmail.com", messageCaptor.getValue().getFrom());
        assertEquals("recipient@example.com", messageCaptor.getValue().getTo()[0]);
        assertTrue(messageCaptor.getValue().getSubject().contains("[TEST]"));
    }

    @Test
    void sendTestEmail_shouldRejectMissingEnvironmentPasswordForAuthenticatedSmtp() {
        when(repository.findCurrent()).thenReturn(Optional.of(persistedSettings()));
        when(smtpSender.isPasswordConfigured()).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.sendTestEmail());

        assertTrue(exception.getMessage().contains("SMTP_PASSWORD"));
    }

    private AlertEmailSettingsRequest validRequest() {
        return new AlertEmailSettingsRequest(
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
                60_000L);
    }

    private AlertEmailSettings persistedSettings() {
        AlertEmailSettings entity = new AlertEmailSettings();
        entity.setId(1L);
        entity.setEnabled(true);
        entity.setFromAddress("sender@gmail.com");
        entity.setToAddress("recipient@example.com");
        entity.setSmtpHost("smtp.gmail.com");
        entity.setSmtpPort(587);
        entity.setSmtpUsername("sender@gmail.com");
        entity.setSmtpAuth(true);
        entity.setStarttlsEnabled(true);
        entity.setStarttlsRequired(true);
        entity.setMaxAttempts(3);
        entity.setRetryDelayMs(60_000L);
        return entity;
    }
}
