package com.example.eventservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eventservice.dto.RsvpRequest;
import com.example.eventservice.dto.RsvpResponse;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.RsvpEntry;
import com.example.eventservice.entity.RsvpStatus;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.RsvpEntryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RsvpServiceTest {

  @Mock private RsvpEntryRepository rsvpEntryRepository;

  @Mock private EventService eventService;

  @InjectMocks private RsvpService rsvpService;

  private Event event;
  private RsvpEntry entry;

  @BeforeEach
  void setUp() {
    event = new Event();
    event.setId(1L);

    entry = new RsvpEntry();
    entry.setId(10L);
    entry.setEvent(event);
    entry.setGuestName("Original Name");
    entry.setContactInfo("original@example.com");
    entry.setStatus(RsvpStatus.PENDING);
  }

  // ---------- create ----------

  @Test
  void create_eventExists_defaultsStatusToPendingWhenNull() {
    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Neha Gupta");
    request.setContactInfo("neha@example.com");
    request.setStatus(null);

    when(eventService.findEvent(1L)).thenReturn(event);
    when(rsvpEntryRepository.save(any(RsvpEntry.class)))
        .thenAnswer(
            invocation -> {
              RsvpEntry saved = invocation.getArgument(0);
              saved.setId(20L);
              return saved;
            });

    RsvpResponse response = rsvpService.create(1L, request);

    ArgumentCaptor<RsvpEntry> captor = ArgumentCaptor.forClass(RsvpEntry.class);
    verify(rsvpEntryRepository).save(captor.capture());
    RsvpEntry saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(RsvpStatus.PENDING);
    assertThat(saved.getEvent()).isEqualTo(event);
    assertThat(response.getGuestName()).isEqualTo("Neha Gupta");
    assertThat(response.getStatus()).isEqualTo(RsvpStatus.PENDING);
    assertThat(response.getEventId()).isEqualTo(1L);
  }

  @Test
  void create_eventExists_honorsExplicitStatus() {
    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Sameer Joshi");
    request.setContactInfo("sameer@example.com");
    request.setStatus(RsvpStatus.CONFIRMED);

    when(eventService.findEvent(1L)).thenReturn(event);
    when(rsvpEntryRepository.save(any(RsvpEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RsvpResponse response = rsvpService.create(1L, request);

    assertThat(response.getStatus()).isEqualTo(RsvpStatus.CONFIRMED);
  }

  @Test
  void create_eventMissing_throwsAndNeverSaves() {
    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Ghost Guest");

    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> rsvpService.create(404L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(rsvpEntryRepository, never()).save(any(RsvpEntry.class));
  }

  // ---------- list ----------

  @Test
  void list_eventExists_returnsPagedRsvps() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<RsvpEntry> page = new PageImpl<>(List.of(entry));

    when(eventService.findEvent(1L)).thenReturn(event);
    when(rsvpEntryRepository.findByEventId(1L, pageable)).thenReturn(page);

    Page<RsvpResponse> result = rsvpService.list(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
  }

  @Test
  void list_eventMissing_throwsResourceNotFoundException() {
    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> rsvpService.list(404L, PageRequest.of(0, 20)))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(rsvpEntryRepository, never()).findByEventId(any(), any());
  }

  // ---------- update ----------

  @Test
  void update_validEntryBelongingToEvent_updatesStatusTransitionPendingToConfirmed() {
    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Updated Name");
    request.setContactInfo("updated@example.com");
    request.setStatus(RsvpStatus.CONFIRMED);

    when(rsvpEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
    when(rsvpEntryRepository.save(any(RsvpEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RsvpResponse response = rsvpService.update(1L, 10L, request);

    assertThat(response.getGuestName()).isEqualTo("Updated Name");
    assertThat(response.getContactInfo()).isEqualTo("updated@example.com");
    assertThat(response.getStatus()).isEqualTo(RsvpStatus.CONFIRMED);
  }

  @Test
  void update_nullStatusInRequest_keepsExistingStatus() {
    entry.setStatus(RsvpStatus.CONFIRMED);
    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Updated Name");
    request.setContactInfo("updated@example.com");
    request.setStatus(null);

    when(rsvpEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
    when(rsvpEntryRepository.save(any(RsvpEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RsvpResponse response = rsvpService.update(1L, 10L, request);

    assertThat(response.getStatus()).isEqualTo(RsvpStatus.CONFIRMED);
  }

  @Test
  void update_entryDoesNotExist_throwsResourceNotFoundException() {
    when(rsvpEntryRepository.findById(999L)).thenReturn(Optional.empty());

    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Whoever");

    assertThatThrownBy(() -> rsvpService.update(1L, 999L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(rsvpEntryRepository, never()).save(any(RsvpEntry.class));
  }

  @Test
  void update_entryBelongsToDifferentEvent_throwsResourceNotFoundException() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    entry.setEvent(otherEvent);

    when(rsvpEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    RsvpRequest request = new RsvpRequest();
    request.setGuestName("Mismatch");

    assertThatThrownBy(() -> rsvpService.update(1L, 10L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("does not belong to event");

    verify(rsvpEntryRepository, never()).save(any(RsvpEntry.class));
  }

  // ---------- delete ----------

  @Test
  void delete_validEntry_deletesIt() {
    when(rsvpEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    rsvpService.delete(1L, 10L);

    verify(rsvpEntryRepository, times(1)).delete(entry);
  }

  @Test
  void delete_entryBelongsToDifferentEvent_throwsAndNeverDeletes() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    entry.setEvent(otherEvent);

    when(rsvpEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    assertThatThrownBy(() -> rsvpService.delete(1L, 10L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(rsvpEntryRepository, never()).delete(any(RsvpEntry.class));
  }

  @Test
  void delete_entryMissing_throwsResourceNotFoundException() {
    when(rsvpEntryRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> rsvpService.delete(1L, 999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(rsvpEntryRepository, never()).delete(any(RsvpEntry.class));
  }
}
