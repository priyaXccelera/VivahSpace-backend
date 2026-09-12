package com.example.proposalservice.dto;

public class BookProposalResponse {
  private ProposalResponse proposal;
  private Long bookingId;
  private Long eventId;

  public BookProposalResponse(ProposalResponse proposal, Long bookingId, Long eventId) {
    this.proposal = proposal;
    this.bookingId = bookingId;
    this.eventId = eventId;
  }

  public ProposalResponse getProposal() {
    return proposal;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public Long getEventId() {
    return eventId;
  }
}
