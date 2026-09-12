package com.example.eventservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.eventservice.dto.ChecklistItemRequest;
import com.example.eventservice.dto.ChecklistItemResponse;
import com.example.eventservice.entity.ChecklistItem;
import com.example.eventservice.entity.Event;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.ChecklistItemRepository;
import java.time.LocalDate;
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
class ChecklistServiceTest {

  @Mock private ChecklistItemRepository checklistItemRepository;

  @Mock private EventService eventService;

  @InjectMocks private ChecklistService checklistService;

  private Event event;
  private ChecklistItem item;

  @BeforeEach
  void setUp() {
    event = new Event();
    event.setId(1L);

    item = new ChecklistItem();
    item.setId(10L);
    item.setEvent(event);
    item.setTitle("Original Title");
    item.setDone(false);
    item.setDueDate(LocalDate.now().plusDays(5));
  }

  // ---------- create ----------

  @Test
  void create_doneNull_defaultsToFalse() {
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Confirm catering headcount");
    request.setDone(null);
    request.setDueDate(LocalDate.now().plusDays(3));

    when(eventService.findEvent(1L)).thenReturn(event);
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(
            invocation -> {
              ChecklistItem saved = invocation.getArgument(0);
              saved.setId(20L);
              return saved;
            });

    ChecklistItemResponse response = checklistService.create(1L, request);

    ArgumentCaptor<ChecklistItem> captor = ArgumentCaptor.forClass(ChecklistItem.class);
    verify(checklistItemRepository).save(captor.capture());
    assertThat(captor.getValue().isDone()).isFalse();
    assertThat(response.isDone()).isFalse();
    assertThat(response.getTitle()).isEqualTo("Confirm catering headcount");
    assertThat(response.getEventId()).isEqualTo(1L);
  }

  @Test
  void create_doneExplicitTrue_setsDoneTrue() {
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Finalize decoration theme");
    request.setDone(true);

    when(eventService.findEvent(1L)).thenReturn(event);
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ChecklistItemResponse response = checklistService.create(1L, request);

    assertThat(response.isDone()).isTrue();
  }

  @Test
  void create_doneExplicitFalse_setsDoneFalse() {
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Book florist");
    request.setDone(false);

    when(eventService.findEvent(1L)).thenReturn(event);
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ChecklistItemResponse response = checklistService.create(1L, request);

    assertThat(response.isDone()).isFalse();
  }

  @Test
  void create_eventMissing_throwsAndNeverSaves() {
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Whatever");

    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> checklistService.create(404L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(checklistItemRepository, never()).save(any(ChecklistItem.class));
  }

  // ---------- list ----------

  @Test
  void list_eventExists_returnsPagedItems() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<ChecklistItem> page = new PageImpl<>(List.of(item));

    when(eventService.findEvent(1L)).thenReturn(event);
    when(checklistItemRepository.findByEventId(1L, pageable)).thenReturn(page);

    Page<ChecklistItemResponse> result = checklistService.list(1L, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
  }

  @Test
  void list_eventMissing_throwsResourceNotFoundException() {
    when(eventService.findEvent(404L))
        .thenThrow(new ResourceNotFoundException("Event not found with id 404"));

    assertThatThrownBy(() -> checklistService.list(404L, PageRequest.of(0, 20)))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(checklistItemRepository, never()).findByEventId(any(), any());
  }

  // ---------- update (done-flag toggling) ----------

  @Test
  void update_toggleDoneFromFalseToTrue() {
    item.setDone(false);
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Original Title");
    request.setDone(true);
    request.setDueDate(item.getDueDate());

    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ChecklistItemResponse response = checklistService.update(1L, 10L, request);

    assertThat(response.isDone()).isTrue();
  }

  @Test
  void update_toggleDoneFromTrueToFalse() {
    item.setDone(true);
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Original Title");
    request.setDone(false);

    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ChecklistItemResponse response = checklistService.update(1L, 10L, request);

    assertThat(response.isDone()).isFalse();
  }

  @Test
  void update_nullDoneInRequest_keepsExistingDoneFlag() {
    item.setDone(true);
    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Renamed title");
    request.setDone(null);

    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));
    when(checklistItemRepository.save(any(ChecklistItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ChecklistItemResponse response = checklistService.update(1L, 10L, request);

    assertThat(response.isDone()).isTrue();
    assertThat(response.getTitle()).isEqualTo("Renamed title");
  }

  @Test
  void update_itemDoesNotExist_throwsResourceNotFoundException() {
    when(checklistItemRepository.findById(999L)).thenReturn(Optional.empty());

    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Whatever");

    assertThatThrownBy(() -> checklistService.update(1L, 999L, request))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(checklistItemRepository, never()).save(any(ChecklistItem.class));
  }

  @Test
  void update_itemBelongsToDifferentEvent_throwsResourceNotFoundException() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    item.setEvent(otherEvent);

    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));

    ChecklistItemRequest request = new ChecklistItemRequest();
    request.setTitle("Whatever");

    assertThatThrownBy(() -> checklistService.update(1L, 10L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("does not belong to event");

    verify(checklistItemRepository, never()).save(any(ChecklistItem.class));
  }

  // ---------- delete ----------

  @Test
  void delete_validItem_deletesIt() {
    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));

    checklistService.delete(1L, 10L);

    verify(checklistItemRepository, times(1)).delete(item);
  }

  @Test
  void delete_itemBelongsToDifferentEvent_throwsAndNeverDeletes() {
    Event otherEvent = new Event();
    otherEvent.setId(2L);
    item.setEvent(otherEvent);

    when(checklistItemRepository.findById(10L)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> checklistService.delete(1L, 10L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(checklistItemRepository, never()).delete(any(ChecklistItem.class));
  }

  @Test
  void delete_itemMissing_throwsResourceNotFoundException() {
    when(checklistItemRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> checklistService.delete(1L, 999L))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(checklistItemRepository, never()).delete(any(ChecklistItem.class));
  }
}
