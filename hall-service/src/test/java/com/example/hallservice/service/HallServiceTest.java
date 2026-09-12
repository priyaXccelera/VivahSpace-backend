package com.example.hallservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.hallservice.dto.HallRequest;
import com.example.hallservice.dto.HallResponse;
import com.example.hallservice.dto.VerificationUpdateRequest;
import com.example.hallservice.entity.Hall;
import com.example.hallservice.entity.VerificationStatus;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.repository.HallRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HallServiceTest {

  @Mock private HallRepository hallRepository;

  private HallService hallService;

  @BeforeEach
  void setUp() {
    hallService = new HallService(hallRepository);
  }

  private HallRequest buildRequest() {
    HallRequest request = new HallRequest();
    request.setOwnerName("Jane Owner");
    request.setName("Grand Hall");
    request.setLocation("Downtown");
    request.setDescription("A lovely hall");
    request.setCapacity(200);
    request.setBasePrice(new BigDecimal("1500.00"));
    return request;
  }

  private Hall buildHall(Long id) {
    Hall hall = new Hall();
    hall.setId(id);
    hall.setOwnerName("Jane Owner");
    hall.setName("Grand Hall");
    hall.setLocation("Downtown");
    hall.setDescription("A lovely hall");
    hall.setCapacity(200);
    hall.setBasePrice(new BigDecimal("1500.00"));
    hall.setVerificationStatus(VerificationStatus.UNVERIFIED);
    return hall;
  }

  @Test
  void create_savesHallAndReturnsResponse() {
    HallRequest request = buildRequest();
    Hall saved = buildHall(1L);
    when(hallRepository.save(any(Hall.class))).thenReturn(saved);

    HallResponse response = hallService.create(request);

    ArgumentCaptor<Hall> captor = ArgumentCaptor.forClass(Hall.class);
    verify(hallRepository).save(captor.capture());
    Hall passed = captor.getValue();
    assertThat(passed.getOwnerName()).isEqualTo("Jane Owner");
    assertThat(passed.getName()).isEqualTo("Grand Hall");
    assertThat(passed.getCapacity()).isEqualTo(200);

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getName()).isEqualTo("Grand Hall");
    assertThat(response.getBasePrice()).isEqualTo(new BigDecimal("1500.00"));
  }

  @Test
  void list_returnsMappedPage() {
    Hall hall = buildHall(1L);
    Pageable pageable = PageRequest.of(0, 20);
    when(hallRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(hall), pageable, 1));

    Page<HallResponse> result = hallService.list(pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("Grand Hall");
  }

  @Test
  void getById_found_returnsResponse() {
    Hall hall = buildHall(5L);
    when(hallRepository.findById(5L)).thenReturn(Optional.of(hall));

    HallResponse response = hallService.getById(5L);

    assertThat(response.getId()).isEqualTo(5L);
  }

  @Test
  void getById_notFound_throwsResourceNotFoundException() {
    when(hallRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hallService.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void update_existingHall_updatesFieldsAndSaves() {
    Hall existing = buildHall(3L);
    when(hallRepository.findById(3L)).thenReturn(Optional.of(existing));
    when(hallRepository.save(any(Hall.class))).thenAnswer(inv -> inv.getArgument(0));

    HallRequest request = buildRequest();
    request.setName("Renamed Hall");

    HallResponse response = hallService.update(3L, request);

    assertThat(response.getName()).isEqualTo("Renamed Hall");
    verify(hallRepository).save(existing);
  }

  @Test
  void update_missingHall_throwsResourceNotFoundException() {
    when(hallRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hallService.update(42L, buildRequest()))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(hallRepository, never()).save(any());
  }

  @Test
  void delete_existingHall_deletesIt() {
    Hall existing = buildHall(7L);
    when(hallRepository.findById(7L)).thenReturn(Optional.of(existing));

    hallService.delete(7L);

    verify(hallRepository).delete(existing);
  }

  @Test
  void delete_missingHall_throwsResourceNotFoundException() {
    when(hallRepository.findById(8L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hallService.delete(8L)).isInstanceOf(ResourceNotFoundException.class);
    verify(hallRepository, never()).delete(any(Hall.class));
  }

  @Test
  void updateVerification_setsStatusAndSaves() {
    Hall existing = buildHall(9L);
    when(hallRepository.findById(9L)).thenReturn(Optional.of(existing));
    when(hallRepository.save(any(Hall.class))).thenAnswer(inv -> inv.getArgument(0));

    VerificationUpdateRequest request = new VerificationUpdateRequest();
    request.setStatus(VerificationStatus.VERIFIED);

    HallResponse response = hallService.updateVerification(9L, request);

    assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    assertThat(existing.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
  }

  @Test
  void findHall_missing_throwsResourceNotFoundException() {
    when(hallRepository.findById(11L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hallService.findHall(11L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("11");
  }
}
