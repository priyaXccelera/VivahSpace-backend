package com.example.eventservice.dto;

import com.example.eventservice.entity.ChecklistItem;
import java.time.LocalDate;

public class ChecklistItemResponse {
  private Long id;
  private Long eventId;
  private String title;
  private boolean done;
  private LocalDate dueDate;

  public static ChecklistItemResponse from(ChecklistItem c) {
    ChecklistItemResponse r = new ChecklistItemResponse();
    r.id = c.getId();
    r.eventId = c.getEvent().getId();
    r.title = c.getTitle();
    r.done = c.isDone();
    r.dueDate = c.getDueDate();
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

  public boolean isDone() {
    return done;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }
}
