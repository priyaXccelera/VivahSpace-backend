package com.example.eventservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eventservice.dto.ItineraryEntryRequest;
import com.example.eventservice.dto.ItineraryEntryResponse;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.ItineraryEntry;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.ItineraryEntryRepository;
import java.time.LocalTime;
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
class ItineraryServiceTest {

  @Mock private ItineraryEntryRepository itineraryEntryRepository;

  @Mock private EventService eventService;

  @InjectMocks private ItineraryService itineraryService;

  private Event event;
  private ItineraryEntry entry;

  @BeforeEach
  void setUp() {
    event = new Event();
    event.setId(1L);

    entry = new ItineraryEntry();
    entry.setId(10L);
    entry.setEvent(event);
    entry.setTitle("Baraat Arrival");
    entry.setStartTime(LocalTime.of(17, 0));
    entry.setEndTime(LocalTime.of(17, 30));
    entry.setDescription("Groom procession arrives at the venue");
  }

  // ---------- create ----------

  @Test
  void create_validTimeOrder_savesEntry() {
    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Dinner");
    request.setStartTime(LocalTime.of(20, 0));
    request.setEndTime(LocalTime.of(22, 0));
    request.setDescription("Multi-cuisine buffet dinner");

    when(eventService.findEvent(1L)).thenReturn(event);
    when(itineraryEntryRepository.save(any(ItineraryEntry.class)))
        .thenAnswer(
            invocation -> {
              ItineraryEntry saved = invocation.getArgument(0);
              saved.setId(20L);
              return saved;
            });

    ItineraryEntryResponse response = itineraryService.create(1L, request);

    ArgumentCaptor<ItineraryEntry> captor = ArgumentCaptor.forClass(ItineraryEntry.class);
    verify(itineraryEntryRepository).save(captor.capture());
    ItineraryEntry saved = captor.getValue();

    assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(20, 0));
    assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(22, 0));
    assertThat(response.getTitle()).isEqualTo("Dinner");
    assertThat(response.getEventId()).isEqualTo(1L);
    // sanity-check the business expectation that end comes after start for a well-formed entry
    assertThat(response.getEndTime()).isAfter(response.getStartTime());
  }

  @Test
  void create_nullEndTime_isAllowedAndPersisted() {
    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Open-ended activity");
    request.setStartTime(LocalTime.of(9, 0));
    request.setEndTime(null);

    when(eventService.findEvent(1L)).thenReturn(event);
    when(itineraryEntryRepository.save(any(ItineraryEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ItineraryEntryResponse response = itineraryService.create(1L, request);

    assertThat(response.getEndTime()).isNull();
    assertThat(response.getStartTime()).isEqualTo(LocalTime.of(9, 0));
  }

  @Test
  void create_eventMissing_throwsAndNeverSaves() {
    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Whatever");
    request.setStartTime(LocalTime.of(10, 0));

    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> itineraryService.create(404L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(itineraryEntryRepository, never()).save(any(ItineraryEntry.class));
  }

  // ---------- list ----------

  @Test
  void list_eventExists_returnsPagedEntriesOrderedAsRepositoryReturns() {
    Pageable pageable = PageRequest.of(0, 20);
    ItineraryEntry secondEntry = new ItineraryEntry();
    secondEntry.setId(11L);
    secondEntry.setEvent(event);
    secondEntry.setTitle("Dinner");
    secondEntry.setStartTime(LocalTime.of(20, 0));
    secondEntry.setEndTime(LocalTime.of(22, 0));

    Page<ItineraryEntry> page = new PageImpl<>(List.of(entry, secondEntry));

    when(eventService.findEvent(1L)).thenReturn(event);
    when(itineraryEntryRepository.findByEventId(1L, pageable)).thenReturn(page);

    Page<ItineraryEntryResponse> result = itineraryService.list(1L, pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().get(0).getStartTime())
        .isBefore(result.getContent().get(1).getStartTime());
  }

  @Test
  void list_eventMissing_throwsResourceNotFoundException() {
    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> itineraryService.list(404L, PageRequest.of(0, 20)))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(itineraryEntryRepository, never()).findByEventId(any(), any());
  }

  // ---------- update ----------

  @Test
  void update_validEntry_updatesTimesAndDescription() {
    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Baraat Arrival (delayed)");
    request.setStartTime(LocalTime.of(18, 0));
    request.setEndTime(LocalTime.of(18, 30));
    request.setDescription("Delayed by traffic");

    when(itineraryEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
    when(itineraryEntryRepository.save(any(ItineraryEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ItineraryEntryResponse response = itineraryService.update(1L, 10L, request);

    assertThat(response.getTitle()).isEqualTo("Baraat Arrival (delayed)");
    assertThat(response.getStartTime()).isEqualTo(LocalTime.of(18, 0));
    assertThat(response.getEndTime()).isEqualTo(LocalTime.of(18, 30));
    assertThat(response.getDescription()).isEqualTo("Delayed by traffic");
  }

  @Test
  void update_entryDoesNotExist_throwsResourceNotFoundException() {
    when(itineraryEntryRepository.findById(999L)).thenReturn(Optional.empty());

    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Whatever");
    request.setStartTime(LocalTime.of(10, 0));

    assertThatThrownBy(() -> itineraryService.update(1L, 999L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(itineraryEntryRepository, never()).save(any(ItineraryEntry.class));
  }

  @Test
  void update_entryBelongsToDifferentEvent_throwsResourceNotFoundException() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    entry.setEvent(otherEvent);

    when(itineraryEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    ItineraryEntryRequest request = new ItineraryEntryRequest();
    request.setTitle("Whatever");
    request.setStartTime(LocalTime.of(10, 0));

    assertThatThrownBy(() -> itineraryService.update(1L, 10L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("does not belong to event");

    verify(itineraryEntryRepository, never()).save(any(ItineraryEntry.class));
  }

  // ---------- delete ----------

  @Test
  void delete_validEntry_deletesIt() {
    when(itineraryEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    itineraryService.delete(1L, 10L);

    verify(itineraryEntryRepository, times(1)).delete(entry);
  }

  @Test
  void delete_entryBelongsToDifferentEvent_throwsAndNeverDeletes() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    entry.setEvent(otherEvent);

    when(itineraryEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

    assertThatThrownBy(() -> itineraryService.delete(1L, 10L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(itineraryEntryRepository, never()).delete(any(ItineraryEntry.class));
  }

  @Test
  void delete_entryMissing_throwsResourceNotFoundException() {
    when(itineraryEntryRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> itineraryService.delete(1L, 999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(itineraryEntryRepository, never()).delete(any(ItineraryEntry.class));
  }
}
