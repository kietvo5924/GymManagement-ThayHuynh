package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.Amenity; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.models.PackageAccessType; // <-- IMPORT MỚI
import com.gym.service.gymmanagementservice.models.PackageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set; // <-- IMPORT MỚI
import java.util.stream.Collectors; // <-- IMPORT MỚI

@Data
@Builder
@Schema(description = "Đối tượng trả về chứa thông tin chi tiết của một gói tập")
public class PackageResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private PackageType packageType;
    private Integer durationDays;
    private Integer numberOfSessions;
    private LocalTime startTimeLimit;
    private LocalTime endTimeLimit;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // --- CÁC TRƯỜNG MỚI ---
    private PackageAccessType accessType;
    private Long targetClubId; // Chỉ trả về ID
    private String targetClubName; // Kèm theo Tên
    private Set<AmenityDTO> amenities; // Trả về DTO của Tiện ích

    // --- Lớp DTO con (nested) ---
    @Data
    @Builder
    public static class AmenityDTO {
        private Long id;
        private String name;

        public static AmenityDTO fromAmenity(Amenity amenity) {
            return AmenityDTO.builder()
                    .id(amenity.getId())
                    .name(amenity.getName())
                    .build();
        }
    }


    public static PackageResponseDTO fromPackage(GymPackage pkg) {
        return PackageResponseDTO.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .packageType(pkg.getPackageType())
                .durationDays(pkg.getDurationDays())
                .numberOfSessions(pkg.getNumberOfSessions())
                .startTimeLimit(pkg.getStartTimeLimit())
                .endTimeLimit(pkg.getEndTimeLimit())
                .isActive(pkg.isActive())
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                // --- CÁC TRƯỜNG MỚI ---
                .accessType(pkg.getAccessType())
                .targetClubId(pkg.getTargetClub() != null ? pkg.getTargetClub().getId() : null)
                .targetClubName(pkg.getTargetClub() != null ? pkg.getTargetClub().getName() : null)
                .amenities(pkg.getAmenities().stream()
                        .map(AmenityDTO::fromAmenity)
                        .collect(Collectors.toSet()))
                // --- KẾT THÚC ---
                .build();
    }
}