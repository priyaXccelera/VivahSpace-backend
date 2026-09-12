package com.example.eventservice.service;

import com.example.eventservice.dto.ChecklistItemRequest;
import com.example.eventservice.dto.ChecklistItemResponse;
import com.example.eventservice.entity.ChecklistItem;
import com.example.eventservice.entity.Event;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.ChecklistItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistService {

  private final ChecklistItemRepository checklistItemRepository;
  private final EventService eventService;

  public ChecklistService(
      ChecklistItemRepository checklistItemRepository, EventService eventService) {
    this.checklistItemRepository = checklistItemRepository;
    this.eventService = eventService;
  }

  @Transactional
  public ChecklistItemResponse create(Long eventId, ChecklistItemRequest request) {
    Event event = eventService.findEvent(eventId);
    ChecklistItem item = new ChecklistItem();
    item.setEvent(event);
    item.setTitle(request.getTitle());
    item.setDone(request.getDone() != null && request.getDone());
    item.setDueDate(request.getDueDate());
    return ChecklistItemResponse.from(checklistItemRepository.save(item));
  }

  @Transactional(readOnly = true)
  public Page<ChecklistItemResponse> list(Long eventId, Pageable pageable) {
    eventService.findEvent(eventId);
    return checklistItemRepository
        .findByEventId(eventId, pageable)
        .map(ChecklistItemResponse::from);
  }

  @Transactional
  public ChecklistItemResponse update(Long eventId, Long itemId, ChecklistItemRequest request) {
    ChecklistItem item = findItem(eventId, itemId);
    item.setTitle(request.getTitle());
    if (request.getDone() != null) {
      item.setDone(request.getDone());
    }
    item.setDueDate(request.getDueDate());
    return ChecklistItemResponse.from(checklistItemRepository.save(item));
  }

  @Transactional
  public void delete(Long eventId, Long itemId) {
    checklistItemRepository.delete(findItem(eventId, itemId));
  }

  private ChecklistItem findItem(Long eventId, Long itemId) {
    ChecklistItem item =
        checklistItemRepository
            .findById(itemId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Checklist item not found with id " + itemId));
    if (!item.getEvent().getId().equals(eventId)) {
      throw new ResourceNotFoundException(
          "Checklist item " + itemId + " does not belong to event " + eventId);
    }
    return item;
  }
}
