package com.example.eventservice.service;

import com.example.eventservice.dto.ItineraryEntryRequest;
import com.example.eventservice.dto.ItineraryEntryResponse;
import com.example.eventservice.entity.Event;
import com.example.eventservice.entity.ItineraryEntry;
import com.example.eventservice.exception.ResourceNotFoundException;
import com.example.eventservice.repository.ItineraryEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItineraryService {

  private final ItineraryEntryRepository itineraryEntryRepository;
  private final EventService eventService;

  public ItineraryService(
      ItineraryEntryRepository itineraryEntryRepository, EventService eventService) {
    this.itineraryEntryRepository = itineraryEntryRepository;
    this.eventService = eventService;
  }

  @Transactional
  public ItineraryEntryResponse create(Long eventId, ItineraryEntryRequest request) {
    Event event = eventService.findEvent(eventId);
    ItineraryEntry entry = new ItineraryEntry();
    entry.setEvent(event);
    entry.setTitle(request.getTitle());
    entry.setStartTime(request.getStartTime());
    entry.setEndTime(request.getEndTime());
    entry.setDescription(request.getDescription());
    return ItineraryEntryResponse.from(itineraryEntryRepository.save(entry));
  }

  @Transactional(readOnly = true)
  public Page<ItineraryEntryResponse> list(Long eventId, Pageable pageable) {
    eventService.findEvent(eventId);
    return itineraryEntryRepository
        .findByEventId(eventId, pageable)
        .map(ItineraryEntryResponse::from);
  }

  @Transactional
  public ItineraryEntryResponse update(Long eventId, Long entryId, ItineraryEntryRequest request) {
    ItineraryEntry entry = findEntry(eventId, entryId);
    entry.setTitle(request.getTitle());
    entry.setStartTime(request.getStartTime());
    entry.setEndTime(request.getEndTime());
    entry.setDescription(request.getDescription());
    return ItineraryEntryResponse.from(itineraryEntryRepository.save(entry));
  }

  @Transactional
  public void delete(Long eventId, Long entryId) {
    itineraryEntryRepository.delete(findEntry(eventId, entryId));
  }

  private ItineraryEntry findEntry(Long eventId, Long entryId) {
    ItineraryEntry entry =
        itineraryEntryRepository
            .findById(entryId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Itinerary entry not found with id " + entryId));
    if (!entry.getEvent().getId().equals(eventId)) {
      throw new ResourceNotFoundException(
          "Itinerary entry " + entryId + " does not belong to event " + eventId);
    }
    return entry;
  }
}
