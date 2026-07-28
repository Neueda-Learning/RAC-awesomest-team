package com.example.monitoring.alert;

import com.example.monitoring.alert.entity.Alert;
import com.example.monitoring.alert.entity.AlertStatusHistory;
import com.example.monitoring.alert.repository.AlertRepository;
import com.example.monitoring.alert.repository.AlertStatusHistoryRepository;
import com.example.monitoring.alert.service.AlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AlertServiceTest {

	@Mock
	private AlertRepository alertRepository;

	@Mock
	private AlertStatusHistoryRepository historyRepository;

	@InjectMocks
	private AlertService alertService;

	@Test
	void acknowledge_shouldTransitionOpenToAcknowledgedAndWriteHistory() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(100L);
		alert.setStatus("OPEN");

		when(alertRepository.findById(100L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.acknowledge(100L, "checked");

		assertEquals("ACKNOWLEDGED", result.getStatus());
		ArgumentCaptor<AlertStatusHistory> captor = ArgumentCaptor.forClass(AlertStatusHistory.class);
		verify(historyRepository).save(captor.capture());
		assertEquals("OPEN", captor.getValue().getOldStatus());
		assertEquals("ACKNOWLEDGED", captor.getValue().getNewStatus());
	}

	@Test
	void acknowledge_shouldThrowWhenStatusIsNotOpen() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(101L);
		alert.setStatus("CLOSED");

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
		alert.setStatus("INVESTIGATING");

		when(alertRepository.findById(102L)).thenReturn(Optional.of(alert));
		when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Alert result = alertService.close(102L, "resolved");

		assertEquals("CLOSED", result.getStatus());
		verify(historyRepository).save(any(AlertStatusHistory.class));
	}

	@Test
	void close_shouldThrowWhenStatusIsAcknowledged() {
		Alert alert = new Alert(1L, 11L, "ACC-001", "HIGH");
		alert.setId(104L);
		alert.setStatus("ACKNOWLEDGED");

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
		alert.setStatus("OPEN");

		when(alertRepository.findById(103L)).thenReturn(Optional.of(alert));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> alertService.startInvestigating(103L, "begin"));
		assertEquals("Can only investigate ACKNOWLEDGED alerts. Current status: OPEN", ex.getMessage());
	}
}
