package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.repositories.GymPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final GymPackageRepository gymPackageRepository;

    @Transactional
    public PackageResponseDTO createPackage(PackageRequestDTO request) {
        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
        });

        GymPackage newGymPackage = GymPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .isActive(true)
                .build();

        GymPackage savedGymPackage = gymPackageRepository.save(newGymPackage);
        return PackageResponseDTO.fromPackage(savedGymPackage);
    }

    public List<PackageResponseDTO> getAllPackages() {
        return gymPackageRepository.findAll().stream()
                .map(PackageResponseDTO::fromPackage)
                .collect(Collectors.toList());
    }

    public PackageResponseDTO getPackageById(Long id) {
        GymPackage pkg = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        return PackageResponseDTO.fromPackage(pkg);
    }

    @Transactional
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
            }
        });

        existingGymPackage.setName(request.getName());
        existingGymPackage.setDescription(request.getDescription());
        existingGymPackage.setPrice(request.getPrice());
        existingGymPackage.setDurationDays(request.getDurationDays());

        GymPackage updatedGymPackage = gymPackageRepository.save(existingGymPackage);
        return PackageResponseDTO.fromPackage(updatedGymPackage);
    }

    @Transactional
    public void togglePackageStatus(Long id) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        existingGymPackage.setActive(!existingGymPackage.isActive());
        gymPackageRepository.save(existingGymPackage);
    }
}
