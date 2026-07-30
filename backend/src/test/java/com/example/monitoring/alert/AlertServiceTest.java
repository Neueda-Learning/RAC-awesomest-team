package com.example.monitoring.alert;

import com.example.monitoring.alert.dto.AlertBulkAction;
import com.example.monitoring.alert.dto.AlertQueryRequest;
import com.example.monitoring.alert.dto.AlertQueryResponse;
import com.example.monitoring.alert.dto.BulkAlertStatusRequest;
import com.example.monitoring.alert.dto.BulkAlertStatusResponse;
import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatus;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.repository.AlertQueryRepository;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.alert.repository.AlertStatusHistoryRepository;
import com.example.monitoring.alert.service.AlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

	@Mock
	private AlertRepository alertRepository;

	@Mock
	private AlertStatusHistoryRepository historyRepository;

	@Mock
	private AlertQueryRepository alertQueryRepository;

	@InjectMocks
	private AlertService alertService;

	@Test
	void acknowledge_shouldTransitionOpenToAcknowledgedAndWriteHistory() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(100L);
		alert.setStatus(AlertStatus.OPEN);

		when(alertRepository.findById(100L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.acknowledge(100L, "checked");

		assertEquals(AlertStatus.ACKNOWLEDGED, result.getStatus());
		ArgumentCaptor<AlertStatusHistory> captor = ArgumentCaptor.forClass(AlertStatusHistory.class);
		verify(historyRepository).save(captor.capture());
		assertEquals(AlertStatus.OPEN, captor.getValue().getOldStatus());
		assertEquals(AlertStatus.ACKNOWLEDGED, captor.getValue().getNewStatus());
	}

	@Test
	void acknowledge_shouldThrowWhenStatusIsNotOpen() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(101L);
		alert.setStatus(AlertStatus.CLOSED);

		when(alertRepository.findById(101L)).thenReturn(Optional.of(alert));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> alertService.acknowledge(101L, null));
		assertEquals("Can only acknowledge OPEN alerts. Current status: CLOSED", ex.getMessage());
		verify(historyRepository, never()).save(any(AlertStatusHistory.class));
	}

	@Test
	void close_shouldAllowInvestigatingToClosed() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(102L);
		alert.setStatus(AlertStatus.INVESTIGATING);

		when(alertRepository.findById(102L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.close(102L, "resolved");

		assertEquals(AlertStatus.CLOSED, result.getStatus());
		verify(historyRepository).save(any(AlertStatusHistory.class));
	}

	@Test
	void close_shouldThrowWhenStatusIsAcknowledged() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(104L);
		alert.setStatus(AlertStatus.ACKNOWLEDGED);

		when(alertRepository.findById(104L)).thenReturn(Optional.of(alert));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> alertService.close(104L, "close directly"));
		assertEquals("Can only close INVESTIGATING alerts. Current status: ACKNOWLEDGED", ex.getMessage());
		verify(historyRepository, never()).save(any(AlertStatusHistory.class));
	}

	@Test
	void startInvestigating_shouldThrowWhenStatusIsInvalid() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(103L);
		alert.setStatus(AlertStatus.OPEN);

		when(alertRepository.findById(103L)).thenReturn(Optional.of(alert));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> alertService.startInvestigating(103L, "begin"));
		assertEquals("Can only investigate ACKNOWLEDGED alerts. Current status: OPEN", ex.getMessage());
	}

	@Test
	void queryAlerts_shouldApplyDefaultsAndDelegateToRepository() {
		AlertQueryRequest request = new AlertQueryRequest();
		Alert sample = new Alert(1L, 2L, "ACC-001", "HIGH");
		sample.setId(55L);
		AlertQueryRepository.AlertQueryResult result = new AlertQueryRepository.AlertQueryResult(List.of(sample), 1L);

		when(alertQueryRepository.query(any(AlertQueryRequest.class))).thenReturn(result);

		AlertQueryResponse response = alertService.queryAlerts(request);

		assertEquals(1L, response.getTotalElements());
		assertEquals(1, response.getTotalPages());
		assertEquals(0, response.getPage());
		assertEquals(20, response.getSize());
		assertEquals(55L, response.getContent().get(0).getId());
		assertEquals("ACC-001", response.getContent().get(0).getAccountId());
		verify(alertQueryRepository).query(any(AlertQueryRequest.class));
	}

	@Test
	void queryAlerts_shouldThrowWhenPageIsNegative() {
		AlertQueryRequest request = new AlertQueryRequest();
		request.setPage(-1);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> alertService.queryAlerts(request));
		assertEquals("page must be greater than or equal to 0", ex.getMessage());
	}

	@Test
	void queryAlerts_shouldThrowWhenDateRangeInvalid() {
		AlertQueryRequest request = new AlertQueryRequest();
		request.setFrom(java.time.LocalDateTime.of(2026, 7, 30, 12, 0));
		request.setTo(java.time.LocalDateTime.of(2026, 7, 30, 10, 0));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> alertService.queryAlerts(request));
		assertEquals("from must not be after to", ex.getMessage());
	}

	@Test
	void acknowledge_shouldMarkSlaBreachedWhenAckIsLate() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(105L);
		alert.setStatus(AlertStatus.OPEN);
		alert.setAckDueAt(LocalDateTime.now().minusMinutes(1));

		when(alertRepository.findById(105L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.acknowledge(105L, "late ack");

		assertNotNull(result.getAckAt());
		assertTrue(Boolean.TRUE.equals(result.getSlaBreached()));
	}

	@Test
	void close_shouldSetResolvedAtAndMarkSlaBreachedWhenResolveIsLate() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(106L);
		alert.setStatus(AlertStatus.INVESTIGATING);
		alert.setResolveDueAt(LocalDateTime.now().minusMinutes(5));

		when(alertRepository.findById(106L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.close(106L, "late close");

		assertNotNull(result.getResolvedAt());
		assertTrue(Boolean.TRUE.equals(result.getSlaBreached()));
	}

	@Test
	void querySlaBreachedAlerts_shouldForceSlaBreachedFilterTrue() {
		Alert sample = new Alert(1L, 2L, "ACC-001", "HIGH");
		sample.setId(66L);
		sample.setSlaBreached(true);
		AlertQueryRepository.AlertQueryResult result = new AlertQueryRepository.AlertQueryResult(List.of(sample), 1L);
		when(alertQueryRepository.query(any(AlertQueryRequest.class))).thenReturn(result);

		alertService.querySlaBreachedAlerts(new AlertQueryRequest());

		ArgumentCaptor<AlertQueryRequest> requestCaptor = ArgumentCaptor.forClass(AlertQueryRequest.class);
		verify(alertQueryRepository).query(requestCaptor.capture());
		assertTrue(Boolean.TRUE.equals(requestCaptor.getValue().getSlaBreached()));
	}

	@Test
	void bulkChangeStatus_shouldContinueAfterInvalidTransition() {
		Alert open = new Alert(1L, 11L, "ACC-001", "HIGH");
		open.setId(201L);
		open.setStatus(AlertStatus.OPEN);
		Alert closed = new Alert(1L, 12L, "ACC-002", "LOW");
		closed.setId(202L);
		closed.setStatus(AlertStatus.CLOSED);

		when(alertRepository.findById(201L)).thenReturn(Optional.of(open));
		when(alertRepository.findById(202L)).thenReturn(Optional.of(closed));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		BulkAlertStatusRequest request = new BulkAlertStatusRequest();
		request.setIds(List.of(201L, 202L));
		request.setAction(AlertBulkAction.ACKNOWLEDGE);
		request.setNotes("  reviewed together  ");

		BulkAlertStatusResponse response = alertService.bulkChangeStatus(request);

		assertEquals(2, response.getRequestedCount());
		assertEquals(1, response.getSuccessCount());
		assertEquals(1, response.getFailureCount());
		assertTrue(response.getResults().get(0).success());
		assertEquals(AlertStatus.ACKNOWLEDGED, response.getResults().get(0).status());
		assertTrue(!response.getResults().get(1).success());
		assertTrue(response.getResults().get(1).error().contains("Current status: CLOSED"));

		ArgumentCaptor<AlertStatusHistory> historyCaptor = ArgumentCaptor.forClass(AlertStatusHistory.class);
		verify(historyRepository).save(historyCaptor.capture());
		assertEquals("reviewed together", historyCaptor.getValue().getNotes());
	}

	@Test
	void bulkChangeStatus_shouldRejectDuplicateIdsBeforeWriting() {
		BulkAlertStatusRequest request = new BulkAlertStatusRequest();
		request.setIds(List.of(201L, 201L));
		request.setAction(AlertBulkAction.ACKNOWLEDGE);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> alertService.bulkChangeStatus(request));

		assertEquals("ids must not contain duplicates", exception.getMessage());
		verify(alertRepository, never()).findById(any(Long.class));
	}

	@Test
	void exportAlertsCsv_shouldReuseFiltersAndEscapeCsvValues() {
		Alert alert = new Alert(7L, 88L, "ACC,\"VIP\"", "HIGH");
		alert.setId(301L);
		alert.setStatus(AlertStatus.INVESTIGATING);
		alert.setDedupCount(2);
		alert.setSlaBreached(true);
		alert.setCreatedAt(LocalDateTime.of(2026, 7, 30, 12, 30));
		when(alertQueryRepository.findForExport(any(AlertQueryRequest.class), eq(5001)))
				.thenReturn(List.of(alert));

		AlertQueryRequest request = new AlertQueryRequest();
		request.setStatusGroup(" active ");
		byte[] bytes = alertService.exportAlertsCsv(request);
		String csv = new String(bytes, StandardCharsets.UTF_8);

		assertTrue(csv.startsWith("\uFEFFid,ruleId,transactionId"));
		assertTrue(csv.contains("\"ACC,\"\"VIP\"\"\""));
		assertTrue(csv.contains("301,7,88"));

		ArgumentCaptor<AlertQueryRequest> requestCaptor = ArgumentCaptor.forClass(AlertQueryRequest.class);
		verify(alertQueryRepository).findForExport(requestCaptor.capture(), eq(5001));
		assertEquals("ACTIVE", requestCaptor.getValue().getStatusGroup());
	}

	@Test
	void exportAlertsCsv_shouldRejectMoreThanMaximumRows() {
		Alert alert = new Alert(1L, 2L, "ACC-001", "LOW");
		when(alertQueryRepository.findForExport(any(AlertQueryRequest.class), eq(5001)))
				.thenReturn(Collections.nCopies(5001, alert));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> alertService.exportAlertsCsv(new AlertQueryRequest()));

		assertTrue(exception.getMessage().contains("maximum of 5000"));
	}
}
