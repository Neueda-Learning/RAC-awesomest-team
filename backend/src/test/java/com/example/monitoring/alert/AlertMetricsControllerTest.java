package com.example.monitoring.alert;

import com.example.monitoring.alert.controller.AlertController;
import com.example.monitoring.alert.dto.AlertAverageResolutionResponse;
import com.example.monitoring.alert.service.AlertMetricsService;
import com.example.monitoring.alert.service.AlertService;
import com.example.monitoring.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertMetricsControllerTest {

    @Mock
    private AlertService alertService;

    @Mock
    private AlertMetricsService metricsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AlertController controller = new AlertController(alertService, metricsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void averageResolution_shouldBindUtcInstants() throws Exception {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(metricsService.getAverageResolution(from, to, "HIGH"))
                .thenReturn(new AlertAverageResolutionResponse(from, to, "HIGH", 2L, 60.0));

        mockMvc.perform(get("/alerts/metrics/average-resolution")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("severity", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAlertCount").value(2))
                .andExpect(jsonPath("$.averageResolutionSeconds").value(60.0));

        verify(metricsService).getAverageResolution(from, to, "HIGH");
    }

    @Test
    void trend_shouldReturnBadRequestForInvalidWindow() throws Exception {
        when(metricsService.getRecentTrend(14, null))
                .thenThrow(new IllegalArgumentException("days must be one of 7, 30"));

        mockMvc.perform(get("/alerts/metrics/trend").param("days", "14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("days must be one of 7, 30"));
    }

    @Test
    void dashboard_shouldReturnBadRequestForInvalidWindow() throws Exception {
        when(metricsService.getDashboardMetrics(14, null))
                .thenThrow(new IllegalArgumentException("days must be one of 7, 30"));

        mockMvc.perform(get("/alerts/metrics/dashboard").param("days", "14"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("days must be one of 7, 30"));
    }

    @Test
    void dashboard_shouldBindCustomUtcDates() throws Exception {
        mockMvc.perform(get("/alerts/metrics/dashboard")
                        .param("from", "2026-06-15")
                        .param("to", "2026-07-30")
                        .param("severity", "HIGH"))
                .andExpect(status().isOk());

        verify(metricsService).getDashboardMetrics(
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 30),
                "HIGH");
    }
}
