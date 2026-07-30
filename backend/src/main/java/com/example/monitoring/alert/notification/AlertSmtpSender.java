package com.example.monitoring.alert.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class AlertSmtpSender {

    private final String smtpPassword;

    public AlertSmtpSender(@Value("${SMTP_PASSWORD:}") String smtpPassword) {
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword;
    }

    public void send(AlertEmailSettingsSnapshot settings, SimpleMailMessage message) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.smtpHost());
        sender.setPort(settings.smtpPort());
        sender.setUsername(trimToNull(settings.smtpUsername()));
        sender.setPassword(settings.smtpAuth() ? smtpPassword : null);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", Boolean.toString(settings.smtpAuth()));
        javaMailProperties.put(
                "mail.smtp.starttls.enable",
                Boolean.toString(settings.starttlsEnabled()));
        javaMailProperties.put(
                "mail.smtp.starttls.required",
                Boolean.toString(settings.starttlsRequired()));
        javaMailProperties.put("mail.smtp.connectiontimeout", "5000");
        javaMailProperties.put("mail.smtp.timeout", "3000");
        javaMailProperties.put("mail.smtp.writetimeout", "5000");

        sender.send(message);
    }

    public boolean isPasswordConfigured() {
        return !smtpPassword.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
