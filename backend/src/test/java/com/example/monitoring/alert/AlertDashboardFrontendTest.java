package com.example.monitoring.alert;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDashboardFrontendTest {

    @Test
    void dashboard_shouldLoadCompactMetricApisInsteadOfRenderingChartsFromAllAlerts() throws IOException {
        try (var stream = getClass().getResourceAsStream("/static/alerts.html")) {
            assertNotNull(stream);
            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(html.contains("/alerts/metrics/dashboard"));
            assertTrue(html.contains("function loadDashboardMetrics"));
            assertTrue(html.contains("id=\"transactionTrendChart\""));
            assertTrue(html.contains("id=\"responseTimeTrendChart\""));
            assertTrue(html.contains("id=\"slaTrendChart\""));
            assertTrue(html.contains("id=\"ruleAlertsChart\""));
            assertFalse(html.contains("id=\"stat-open\""));
            assertFalse(html.contains("id=\"stat-ack\""));
            assertFalse(html.contains("id=\"stat-inv\""));
            assertFalse(html.contains("id=\"stat-closed\""));
            assertFalse(html.contains("id=\"stat-dismissed\""));

            int renderStart = html.indexOf("function renderDashboard(");
            int nextFunction = html.indexOf("function renderPending(", renderStart);
            String renderDashboard = html.substring(renderStart, nextFunction);
            assertFalse(renderDashboard.contains("allAlerts"));
        }
    }
}
