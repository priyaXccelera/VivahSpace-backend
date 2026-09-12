package com.example.eventservice.service;

import com.example.eventservice.dto.EventRequest;
import com.example.eventservice.dto.EventResponse;
import com.example.eventservice.dto.GuestCountUpdateRequest;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.EventStatus;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private final EventRepository eventRepository;

  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  @Transactional
  public EventResponse create(EventRequest request) {
    Event event = new Event();
    event.setProposalId(request.getProposalId());
    event.setHallId(request.getHallId());
    event.setUserId(request.getUserId());
    event.setGuestCount(request.getGuestCount());
    event.setStatus(EventStatus.ACTIVE);
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional(readOnly = true)
  public Page<EventResponse> list(Long userId, Pageable pageable) {
    Page<Event> page =
        userId != null
            ? eventRepository.findByUserId(userId, pageable)
            : eventRepository.findAll(pageable);
    return page.map(EventResponse::from);
  }

  @Transactional(readOnly = true)
  public EventResponse getById(Long id) {
    return EventResponse.from(findEvent(id));
  }

  @Transactional
  public EventResponse updateGuestCount(Long id, GuestCountUpdateRequest request) {
    Event event = findEvent(id);
    event.setGuestCount(request.getGuestCount());
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional
  public EventResponse updateStatus(Long id, EventStatus status) {
    Event event = findEvent(id);
    event.setStatus(status);
    return EventResponse.from(eventRepository.save(event));
  }

  @Transactional
  public void delete(Long id) {
    eventRepository.delete(findEvent(id));
  }

  public Event findEvent(Long id) {
    return eventRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));
  }
}
