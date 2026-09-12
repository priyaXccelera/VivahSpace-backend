package com.example.eventservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rsvp_entries")
public class RsvpEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Column(nullable = false, length = 150)
  private String guestName;

  @Column(length = 150)
  private String contactInfo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RsvpStatus status = RsvpStatus.PENDING;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public String getContactInfo() {
    return contactInfo;
  }

  public void setContactInfo(String contactInfo) {
    this.contactInfo = contactInfo;
  }

  public RsvpStatus getStatus() {
    return status;
  }

  public void setStatus(RsvpStatus status) {
    this.status = status;
  }
}
