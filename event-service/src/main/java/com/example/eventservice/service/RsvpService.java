package com.example.eventservice.service;

import com.example.eventservice.dto.RsvpRequest;
import com.example.eventservice.dto.RsvpResponse;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.RsvpEntry;
import com.example.eventservice.entity.RsvpStatus;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.RsvpEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RsvpService {

  private final RsvpEntryRepository rsvpEntryRepository;
  private final EventService eventService;

  public RsvpService(RsvpEntryRepository rsvpEntryRepository, EventService eventService) {
    this.rsvpEntryRepository = rsvpEntryRepository;
    this.eventService = eventService;
  }

  @Transactional
  public RsvpResponse create(Long eventId, RsvpRequest request) {
    Event event = eventService.findEvent(eventId);
    RsvpEntry entry = new RsvpEntry();
    entry.setEvent(event);
    entry.setGuestName(request.getGuestName());
    entry.setContactInfo(request.getContactInfo());
    entry.setStatus(request.getStatus() != null ? request.getStatus() : RsvpStatus.PENDING);
    return RsvpResponse.from(rsvpEntryRepository.save(entry));
  }

  @Transactional(readOnly = true)
  public Page<RsvpResponse> list(Long eventId, Pageable pageable) {
    eventService.findEvent(eventId);
    return rsvpEntryRepository.findByEventId(eventId, pageable).map(RsvpResponse::from);
  }

  @Transactional
  public RsvpResponse update(Long eventId, Long rsvpId, RsvpRequest request) {
    RsvpEntry entry = findEntry(eventId, rsvpId);
    entry.setGuestName(request.getGuestName());
    entry.setContactInfo(request.getContactInfo());
    if (request.getStatus() != null) {
      entry.setStatus(request.getStatus());
    }
    return RsvpResponse.from(rsvpEntryRepository.save(entry));
  }

  @Transactional
  public void delete(Long eventId, Long rsvpId) {
    rsvpEntryRepository.delete(findEntry(eventId, rsvpId));
  }

  private RsvpEntry findEntry(Long eventId, Long rsvpId) {
    RsvpEntry entry =
        rsvpEntryRepository
            .findById(rsvpId)
            .orElseThrow(() -> new ResourceNotFoundException("RSVP not found with id " + rsvpId));
    if (!entry.getEvent().getId().equals(eventId)) {
      throw new ResourceNotFoundException(
          "RSVP " + rsvpId + " does not belong to event " + eventId);
    }
    return entry;
  }
}
