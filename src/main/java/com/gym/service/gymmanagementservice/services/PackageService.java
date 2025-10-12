package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.Package;
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

        Package newPackage = Package.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .isActive(true)
                .build();

        Package savedPackage = packageRepository.save(newPackage);
        return PackageResponseDTO.fromPackage(savedPackage);
    }

    public List<PackageResponseDTO> getAllPackages() {
        return packageRepository.findAll().stream()
                .map(PackageResponseDTO::fromPackage)
                .collect(Collectors.toList());
    }

    public PackageResponseDTO getPackageById(Long id) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        return PackageResponseDTO.fromPackage(pkg);
    }

    @Transactional
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        Package existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        packageRepository.findByName(request.getName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
            }
        });

        existingPackage.setName(request.getName());
        existingPackage.setDescription(request.getDescription());
        existingPackage.setPrice(request.getPrice());
        existingPackage.setDurationDays(request.getDurationDays());

        Package updatedPackage = packageRepository.save(existingPackage);
        return PackageResponseDTO.fromPackage(updatedPackage);
    }

    @Transactional
    public void togglePackageStatus(Long id) {
        Package existingPackage = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        existingPackage.setActive(!existingPackage.isActive());
        packageRepository.save(existingPackage);
    }
}
