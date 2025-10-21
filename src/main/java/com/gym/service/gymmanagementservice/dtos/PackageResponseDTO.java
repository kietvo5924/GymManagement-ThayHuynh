package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.GymPackage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về chứa thông tin chi tiết của một gói tập")
public class PackageResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static PackageResponseDTO fromPackage(GymPackage pkg) {
        return PackageResponseDTO.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .durationDays(pkg.getDurationDays())
                .isActive(pkg.isActive())
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                .build();
    }
}
