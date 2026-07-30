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
            assertTrue(html.contains("value=\"custom\">Custom date range"));
            assertTrue(html.contains("id=\"dashboard-from\""));
            assertTrue(html.contains("id=\"dashboard-to\""));
            assertTrue(html.contains("function toggleDashboardDateInputs("));
            assertTrue(html.contains("params.set('from', from)"));
            assertTrue(html.contains("params.set('to', to)"));
            assertFalse(html.contains("id=\"stat-open\""));
            assertFalse(html.contains("id=\"stat-ack\""));
            assertFalse(html.contains("id=\"stat-inv\""));
            assertFalse(html.contains("id=\"stat-closed\""));
            assertFalse(html.contains("id=\"stat-dismissed\""));
            assertTrue(html.contains("id=\"pending-select-all\""));
            assertTrue(html.contains("function bulkChangeStatus("));
            assertTrue(html.contains("/alerts/bulk/status"));
            assertTrue(html.contains("function exportAlerts("));
            assertTrue(html.contains("/alerts/export?"));
            assertTrue(html.contains("<th>Occurrences</th>"));
            assertTrue(html.contains("<th>Last Triggered</th>"));
            assertTrue(html.contains("id=\"alert-transactions-modal\""));
            assertTrue(html.contains("function openAlertTransactions("));
            assertTrue(html.contains("/transactions`"));
            assertFalse(html.contains("function toggleDetail("));
            assertTrue(html.contains("id=\"page-email-settings\""));
            assertTrue(html.contains("id=\"email-settings-form\""));
            assertTrue(html.contains("/admin/email-settings"));
            assertTrue(html.contains("/admin/email-settings/test"));
            assertTrue(html.contains("function loadEmailSettings("));
            assertTrue(html.contains("function saveEmailSettings("));
            assertTrue(html.contains("function sendTestEmail("));
            assertTrue(html.contains("SMTP_PASSWORD environment variable"));
            assertTrue(html.contains(".env</code> file is not automatically loaded"));
            assertFalse(html.contains("id=\"email-smtp-password\""));
            assertFalse(html.contains("name=\"smtpPassword\""));

            int renderStart = html.indexOf("function renderDashboard(");
            int nextFunction = html.indexOf("function renderPending(", renderStart);
            String renderDashboard = html.substring(renderStart, nextFunction);
            assertFalse(renderDashboard.contains("allAlerts"));
        }
    }
}
