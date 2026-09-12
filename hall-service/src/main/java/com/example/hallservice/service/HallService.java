package com.example.hallservice.service;

import com.example.hallservice.dto.HallRequest;
import com.example.hallservice.dto.HallResponse;
import com.example.hallservice.dto.VerificationUpdateRequest;
import com.example.hallservice.entity.Hall;
import com.example.hallservice.exception.ResourceNotFoundException;
import com.example.hallservice.repository.HallRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HallService {

  private final HallRepository hallRepository;

  public HallService(HallRepository hallRepository) {
    this.hallRepository = hallRepository;
  }

  @Transactional
  public HallResponse create(HallRequest request) {
    Hall hall = new Hall();
    apply(hall, request);
    return HallResponse.from(hallRepository.save(hall));
  }

  @Transactional(readOnly = true)
  public Page<HallResponse> list(Pageable pageable) {
    return hallRepository.findAll(pageable).map(HallResponse::from);
  }

  @Transactional(readOnly = true)
  public HallResponse getById(Long id) {
    return HallResponse.from(findHall(id));
  }

  @Transactional
  public HallResponse update(Long id, HallRequest request) {
    Hall hall = findHall(id);
    apply(hall, request);
    return HallResponse.from(hallRepository.save(hall));
  }

  @Transactional
  public void delete(Long id) {
    Hall hall = findHall(id);
    hallRepository.delete(hall);
  }

  @Transactional
  public HallResponse updateVerification(Long id, VerificationUpdateRequest request) {
    Hall hall = findHall(id);
    hall.setVerificationStatus(request.getStatus());
    return HallResponse.from(hallRepository.save(hall));
  }

  public Hall findHall(Long id) {
    return hallRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Hall not found with id " + id));
  }

  private void apply(Hall hall, HallRequest request) {
    hall.setOwnerName(request.getOwnerName());
    hall.setName(request.getName());
    hall.setLocation(request.getLocation());
    hall.setDescription(request.getDescription());
    hall.setCapacity(request.getCapacity());
    hall.setBasePrice(request.getBasePrice());
  }
}
