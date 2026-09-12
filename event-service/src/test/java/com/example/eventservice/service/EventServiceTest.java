package com.example.eventservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.GuestCountUpdateRequest;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.EventStatus;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.EventRepository;
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
class EventServiceTest {

  @Mock private EventRepository eventRepository;

  @InjectMocks private EventService eventService;

  private Event sampleEvent;

  @BeforeEach
  void setUp() {
    sampleEvent = new Event();
    sampleEvent.setId(1L);
    sampleEvent.setProposalId(10L);
    sampleEvent.setHallId(20L);
    sampleEvent.setUserId(30L);
    sampleEvent.setGuestCount(100);
    sampleEvent.setStatus(EventStatus.ACTIVE);
  }

  // ---------- create ----------

  @Test
  void create_savesNewEventWithActiveStatus() {
    EventRequest request = new EventRequest();
    request.setProposalId(10L);
    request.setHallId(20L);
    request.setUserId(30L);
    request.setGuestCount(150);

    when(eventRepository.save(any(Event.class)))
        .thenAnswer(
            invocation -> {
              Event e = invocation.getArgument(0);
              e.setId(5L);
              return e;
            });

    EventResponse response = eventService.create(request);

    ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(captor.capture());
    Event saved = captor.getValue();

    assertThat(saved.getProposalId()).isEqualTo(10L);
    assertThat(saved.getHallId()).isEqualTo(20L);
    assertThat(saved.getUserId()).isEqualTo(30L);
    assertThat(saved.getGuestCount()).isEqualTo(150);
    assertThat(saved.getStatus()).isEqualTo(EventStatus.ACTIVE);

    assertThat(response.getId()).isEqualTo(5L);
    assertThat(response.getGuestCount()).isEqualTo(150);
    assertThat(response.getStatus()).isEqualTo(EventStatus.ACTIVE);
  }

  // ---------- list ----------

  @Test
  void list_withUserId_delegatesToFindByUserId() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Event> page = new PageImpl<>(List.of(sampleEvent));
    when(eventRepository.findByUserId(eq(30L), eq(pageable))).thenReturn(page);

    Page<EventResponse> result = eventService.list(30L, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    verify(eventRepository).findByUserId(30L, pageable);
    verify(eventRepository, never()).findAll(any(Pageable.class));
  }

  @Test
  void list_withoutUserId_delegatesToFindAll() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Event> page = new PageImpl<>(List.of(sampleEvent));
    when(eventRepository.findAll(eq(pageable))).thenReturn(page);

    Page<EventResponse> result = eventService.list(null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(eventRepository).findAll(pageable);
    verify(eventRepository, never()).findByUserId(anyLong(), any(Pageable.class));
  }

  // ---------- getById ----------

  @Test
  void getById_found_returnsResponse() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

    EventResponse response = eventService.getById(1L);

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getGuestCount()).isEqualTo(100);
  }

  @Test
  void getById_notFound_throwsResourceNotFoundException() {
    when(eventRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  // ---------- updateGuestCount ----------

  @Test
  void updateGuestCount_eventExists_updatesAndSaves() {
    GuestCountUpdateRequest request = new GuestCountUpdateRequest();
    request.setGuestCount(275);

    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EventResponse response = eventService.updateGuestCount(1L, request);

    assertThat(response.getGuestCount()).isEqualTo(275);
    assertThat(sampleEvent.getGuestCount()).isEqualTo(275);
    verify(eventRepository).save(sampleEvent);
  }

  @Test
  void updateGuestCount_eventMissing_throwsAndNeverSaves() {
    GuestCountUpdateRequest request = new GuestCountUpdateRequest();
    request.setGuestCount(275);

    when(eventRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.updateGuestCount(404L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(eventRepository, never()).save(any(Event.class));
  }

  // ---------- updateStatus ----------

  @Test
  void updateStatus_eventExists_transitionsToCompleted() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EventResponse response = eventService.updateStatus(1L, EventStatus.COMPLETED);

    assertThat(response.getStatus()).isEqualTo(EventStatus.COMPLETED);
    assertThat(sampleEvent.getStatus()).isEqualTo(EventStatus.COMPLETED);
  }

  @Test
  void updateStatus_eventExists_transitionsToCancelled() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
    when(eventRepository.save(any(Event.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EventResponse response = eventService.updateStatus(1L, EventStatus.CANCELLED);

    assertThat(response.getStatus()).isEqualTo(EventStatus.CANCELLED);
  }

  @Test
  void updateStatus_eventMissing_throwsResourceNotFoundException() {
    when(eventRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.updateStatus(404L, EventStatus.CANCELLED))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(eventRepository, never()).save(any(Event.class));
  }

  // ---------- delete ----------

  @Test
  void delete_eventExists_deletesIt() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

    eventService.delete(1L);

    verify(eventRepository, times(1)).delete(sampleEvent);
  }

  @Test
  void delete_eventMissing_throwsAndNeverDeletes() {
    when(eventRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.delete(404L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(eventRepository, never()).delete(any(Event.class));
  }

  // ---------- findEvent ----------

  @Test
  void findEvent_found_returnsEntity() {
    when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));

    Event found = eventService.findEvent(1L);

    assertThat(found).isSameAs(sampleEvent);
  }

  @Test
  void findEvent_notFound_throwsResourceNotFoundException() {
    when(eventRepository.findById(2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.findEvent(2L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("2");
  }
}
