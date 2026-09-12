package com.example.eventservice.dto;

import com.example.eventservice.entity.RsvpEntry;
import com.example.eventservice.entity.RsvpStatus;

public class RsvpResponse {
  private Long id;
  private Long eventId;
  private String guestName;
  private String contactInfo;
  private RsvpStatus status;

  public static RsvpResponse from(RsvpEntry r) {
    RsvpResponse resp = new RsvpResponse();
    resp.id = r.getId();
    resp.eventId = r.getEvent().getId();
    resp.guestName = r.getGuestName();
    resp.contactInfo = r.getContactInfo();
    resp.status = r.getStatus();
    return resp;
  }

  public Long getId() {
    return id;
  }

  public Long getEventId() {
    return eventId;
  }

  public String getGuestName() {
    return guestName;
  }

  public String getContactInfo() {
    return contactInfo;
  }

  public RsvpStatus getStatus() {
    return status;
  }
}
