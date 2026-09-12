package com.example.hallservice.service;

import com.example.hallservice.dto.*;
import com.example.hallservice.entity.*;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.exception.SlotUnavailableException;
import com.example.hallservice.repository.HallAvailabilitySlotRepository;
import com.example.hallservice.repository.HallRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

  private final HallAvailabilitySlotRepository slotRepository;
  private final HallRepository hallRepository;
  private final HallService hallService;

  public AvailabilityService(
      HallAvailabilitySlotRepository slotRepository,
      HallRepository hallRepository,
      HallService hallService) {
    this.slotRepository = slotRepository;
    this.hallRepository = hallRepository;
    this.hallService = hallService;
  }

  @Transactional
  public SlotResponse createSlot(Long hallId, SlotRequest request) {
    Hall hall = hallService.findHall(hallId);
    if (slotRepository.existsByHallIdAndSlotDateAndSlotType(
        hallId, request.getSlotDate(), request.getSlotType())) {
      throw new SlotUnavailableException(
          "A slot of type "
              + request.getSlotType()
              + " already exists for this hall on "
              + request.getSlotDate());
    }
    HallAvailabilitySlot slot = new HallAvailabilitySlot();
    slot.setHall(hall);
    slot.setSlotDate(request.getSlotDate());
    slot.setSlotType(request.getSlotType());
    slot.setStatus(SlotStatus.AVAILABLE);
    return SlotResponse.from(slotRepository.save(slot));
  }

  @Transactional(readOnly = true)
  public Page<SlotResponse> listSlots(Long hallId, Pageable pageable) {
    hallService.findHall(hallId);
    return slotRepository.findByHallId(hallId, pageable).map(SlotResponse::from);
  }

  @Transactional(readOnly = true)
  public AvailabilityQueryResponse queryAvailability(Long hallId, java.time.LocalDate date) {
    hallService.findHall(hallId);
    List<HallAvailabilitySlot> available =
        slotRepository.findByHallIdAndSlotDateAndStatus(hallId, date, SlotStatus.AVAILABLE);
    List<SlotResponse> availableSlots =
        available.stream().map(SlotResponse::from).collect(Collectors.toList());

    List<HallResponse> alternatives = List.of();
    if (availableSlots.isEmpty()) {
      alternatives =
          slotRepository.findBySlotDateAndStatus(date, SlotStatus.AVAILABLE).stream()
              .filter(s -> !s.getHall().getId().equals(hallId))
              .map(s -> s.getHall())
              .distinct()
              .map(HallResponse::from)
              .collect(Collectors.toList());
    }
    return new AvailabilityQueryResponse(hallId, availableSlots, alternatives);
  }

  public HallAvailabilitySlot findSlot(Long slotId) {
    return slotRepository
        .findById(slotId)
        .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id " + slotId));
  }
}
