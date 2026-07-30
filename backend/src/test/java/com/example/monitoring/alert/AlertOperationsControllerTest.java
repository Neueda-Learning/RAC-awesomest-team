package com.example.monitoring.alert;

import com.example.monitoring.alert.controller.AlertController;
import com.example.monitoring.alert.dto.AlertBulkAction;
import com.example.monitoring.alert.dto.BulkAlertStatusRequest;
import com.example.monitoring.alert.dto.BulkAlertStatusResponse;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.service.AlertMetricsService;
import com.example.monitoring.alert.service.AlertService;
import com.example.monitoring.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertOperationsControllerTest {

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
    void bulkStatus_shouldReturnPerItemOutcome() throws Exception {
        BulkAlertStatusResponse response = new BulkAlertStatusResponse(
                2,
                1,
                1,
                List.of(
                        new BulkAlertStatusResponse.ItemResult(1L, true, AlertStatus.ACKNOWLEDGED, null),
                        new BulkAlertStatusResponse.ItemResult(2L, false, null, "invalid transition")
                ));
        when(alertService.bulkChangeStatus(any(BulkAlertStatusRequest.class))).thenReturn(response);

        mockMvc.perform(post("/alerts/bulk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[1,2],"action":"ACKNOWLEDGE","notes":"reviewed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.results[1].error").value("invalid transition"));
    }

    @Test
    void export_shouldReturnDownloadableUtf8Csv() throws Exception {
        byte[] csv = "\uFEFFid,status\r\n1,OPEN\r\n".getBytes(StandardCharsets.UTF_8);
        when(alertService.exportAlertsCsv(any())).thenReturn(csv);

        mockMvc.perform(get("/alerts/export")
                        .param("statusGroup", "ACTIVE")
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern(
                                "attachment; filename=\"alerts-\\d{8}-\\d{6}\\.csv\"")))
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().bytes(csv));
    }

    @Test
    void bulkStatus_shouldRejectEmptyIds() throws Exception {
        mockMvc.perform(post("/alerts/bulk/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[],"action":"ACKNOWLEDGE"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
