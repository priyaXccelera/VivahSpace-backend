package com.example.hallservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hallservice.dto.AvailabilityQueryResponse;
import com.example.hallservice.dto.SlotRequest;
import com.example.hallservice.dto.SlotResponse;
import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.HallAvailabilitySlot;
import com.example.hallservice.entity.SlotStatus;
import com.example.hallservice.entity.SlotType;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.exception.SlotUnavailableException;
import com.example.hallservice.repository.HallAvailabilitySlotRepository;
import com.example.hallservice.repository.HallRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

  @Mock private HallAvailabilitySlotRepository slotRepository;

  @Mock private HallRepository hallRepository;

  @Mock private HallService hallService;

  private AvailabilityService availabilityService;

  @BeforeEach
  void setUp() {
    availabilityService = new AvailabilityService(slotRepository, hallRepository, hallService);
  }

  private Hall buildHall(Long id, String name) {
    Hall hall = new Hall();
    hall.setId(id);
    hall.setName(name);
    return hall;
  }

  private HallAvailabilitySlot buildSlot(
      Long id, Hall hall, LocalDate date, SlotType type, SlotStatus status) {
    HallAvailabilitySlot slot = new HallAvailabilitySlot();
    slot.setId(id);
    slot.setHall(hall);
    slot.setSlotDate(date);
    slot.setSlotType(type);
    slot.setStatus(status);
    return slot;
  }

  @Test
  void createSlot_success_savesAvailableSlot() {
    Hall hall = buildHall(1L, "Grand Hall");
    when(hallService.findHall(1L)).thenReturn(hall);
    LocalDate date = LocalDate.of(2024, 12, 25);
    when(slotRepository.existsByHallIdAndSlotDateAndSlotType(1L, date, SlotType.EVENING))
        .thenReturn(false);
    when(slotRepository.save(any(HallAvailabilitySlot.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SlotRequest request = new SlotRequest();
    request.setSlotDate(date);
    request.setSlotType(SlotType.EVENING);

    SlotResponse response = availabilityService.createSlot(1L, request);

    assertThat(response.getHallId()).isEqualTo(1L);
    assertThat(response.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(response.getSlotType()).isEqualTo(SlotType.EVENING);
  }

  @Test
  void createSlot_duplicateSlot_throwsSlotUnavailableException() {
    Hall hall = buildHall(1L, "Grand Hall");
    when(hallService.findHall(1L)).thenReturn(hall);
    LocalDate date = LocalDate.of(2024, 12, 25);
    when(slotRepository.existsByHallIdAndSlotDateAndSlotType(1L, date, SlotType.EVENING))
        .thenReturn(true);

    SlotRequest request = new SlotRequest();
    request.setSlotDate(date);
    request.setSlotType(SlotType.EVENING);

    assertThatThrownBy(() -> availabilityService.createSlot(1L, request))
        .isInstanceOf(SlotUnavailableException.class);
    verify(slotRepository, never()).save(any());
  }

  @Test
  void createSlot_hallMissing_propagatesResourceNotFoundException() {
    when(hallService.findHall(99L))
        .thenThrow(new ResourceNotFoundException("Hall not found with id 99"));

    SlotRequest request = new SlotRequest();
    request.setSlotDate(LocalDate.now());
    request.setSlotType(SlotType.MORNING);

    assertThatThrownBy(() -> availabilityService.createSlot(99L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void listSlots_returnsMappedPage() {
    Hall hall = buildHall(1L, "Grand Hall");
    when(hallService.findHall(1L)).thenReturn(hall);
    Pageable pageable = PageRequest.of(0, 20);
    HallAvailabilitySlot slot =
        buildSlot(1L, hall, LocalDate.now(), SlotType.MORNING, SlotStatus.AVAILABLE);
    when(slotRepository.findByHallId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(slot), pageable, 1));

    Page<SlotResponse> result = availabilityService.listSlots(1L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void queryAvailability_slotsAvailable_returnsNoAlternatives() {
    Hall hall = buildHall(1L, "Grand Hall");
    when(hallService.findHall(1L)).thenReturn(hall);
    LocalDate date = LocalDate.of(2024, 12, 25);
    HallAvailabilitySlot slot = buildSlot(1L, hall, date, SlotType.MORNING, SlotStatus.AVAILABLE);
    when(slotRepository.findByHallIdAndSlotDateAndStatus(1L, date, SlotStatus.AVAILABLE))
        .thenReturn(List.of(slot));

    AvailabilityQueryResponse response = availabilityService.queryAvailability(1L, date);

    assertThat(response.getAvailableSlots()).hasSize(1);
    assertThat(response.getSuggestedAlternatives()).isEmpty();
    verify(slotRepository, never()).findBySlotDateAndStatus(any(), any());
  }

  @Test
  void queryAvailability_noSlots_suggestsAlternativeHalls() {
    Hall hall = buildHall(1L, "Grand Hall");
    Hall otherHall = buildHall(2L, "Other Hall");
    when(hallService.findHall(1L)).thenReturn(hall);
    LocalDate date = LocalDate.of(2024, 12, 25);
    when(slotRepository.findByHallIdAndSlotDateAndStatus(1L, date, SlotStatus.AVAILABLE))
        .thenReturn(List.of());
    HallAvailabilitySlot otherSlot =
        buildSlot(2L, otherHall, date, SlotType.EVENING, SlotStatus.AVAILABLE);
    when(slotRepository.findBySlotDateAndStatus(date, SlotStatus.AVAILABLE))
        .thenReturn(List.of(otherSlot));

    AvailabilityQueryResponse response = availabilityService.queryAvailability(1L, date);

    assertThat(response.getAvailableSlots()).isEmpty();
    assertThat(response.getSuggestedAlternatives()).hasSize(1);
    assertThat(response.getSuggestedAlternatives().get(0).getId()).isEqualTo(2L);
  }

  @Test
  void findSlot_found_returnsSlot() {
    Hall hall = buildHall(1L, "Grand Hall");
    HallAvailabilitySlot slot =
        buildSlot(5L, hall, LocalDate.now(), SlotType.MORNING, SlotStatus.AVAILABLE);
    when(slotRepository.findById(5L)).thenReturn(Optional.of(slot));

    HallAvailabilitySlot found = availabilityService.findSlot(5L);

    assertThat(found.getId()).isEqualTo(5L);
  }

  @Test
  void findSlot_missing_throwsResourceNotFoundException() {
    when(slotRepository.findById(123L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> availabilityService.findSlot(123L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("123");
  }
}
