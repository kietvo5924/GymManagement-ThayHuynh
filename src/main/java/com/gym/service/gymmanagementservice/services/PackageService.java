package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.repositories.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;

    @Transactional
    public PackageResponseDTO createPackage(PackageRequestDTO request) {
        packageRepository.findByName(request.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
        });

        GymPackage newGymPackage = GymPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .isActive(true)
                .build();

        GymPackage savedGymPackage = packageRepository.save(newGymPackage);
        return PackageResponseDTO.fromPackage(savedGymPackage);
    }

    public List<PackageResponseDTO> getAllPackages() {
        return packageRepository.findAll().stream()
                .map(PackageResponseDTO::fromPackage)
                .collect(Collectors.toList());
    }

    public PackageResponseDTO getPackageById(Long id) {
        GymPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        return PackageResponseDTO.fromPackage(pkg);
    }

    @Transactional
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        GymPackage existingGymPackage = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        packageRepository.findByName(request.getName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
            }
        });

        existingGymPackage.setName(request.getName());
        existingGymPackage.setDescription(request.getDescription());
        existingGymPackage.setPrice(request.getPrice());
        existingGymPackage.setDurationDays(request.getDurationDays());

        GymPackage updatedGymPackage = packageRepository.save(existingGymPackage);
        return PackageResponseDTO.fromPackage(updatedGymPackage);
    }

    @Transactional
    public void togglePackageStatus(Long id) {
        GymPackage existingGymPackage = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        existingGymPackage.setActive(!existingGymPackage.isActive());
        packageRepository.save(existingGymPackage);
    }
}
