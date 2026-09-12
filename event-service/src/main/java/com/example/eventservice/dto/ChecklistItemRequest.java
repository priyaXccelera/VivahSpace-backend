package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class ChecklistItemRequest {
  @NotBlank private String title;
  private Boolean done;
  private LocalDate dueDate;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getDone() {
    return done;
  }

  public void setDone(Boolean done) {
    this.done = done;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }
}
