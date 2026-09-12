package com.example.eventservice.dto;

import com.example.eventservice.entity.ItineraryEntry;
import java.time.LocalTime;

public class ItineraryEntryResponse {
  private Long id;
  private Long eventId;
  private String title;
  private LocalTime startTime;
  private LocalTime endTime;
  private String description;

  public static ItineraryEntryResponse from(ItineraryEntry i) {
    ItineraryEntryResponse r = new ItineraryEntryResponse();
    r.id = i.getId();
    r.eventId = i.getEvent().getId();
    r.title = i.getTitle();
    r.startTime = i.getStartTime();
    r.endTime = i.getEndTime();
    r.description = i.getDescription();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getEventId() {
    return eventId;
  }

  public String getTitle() {
    return title;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public String getDescription() {
    return description;
  }
}
