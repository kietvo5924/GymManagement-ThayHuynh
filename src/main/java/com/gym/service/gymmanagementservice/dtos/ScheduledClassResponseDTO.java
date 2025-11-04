package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.ScheduledClass;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ScheduledClassResponseDTO {
    private Long id;
    private String className;
    private String instructorName;
    private String clubName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer maxCapacity;
    // private Integer currentBookings; // Sẽ thêm ở bước sau

    public static ScheduledClassResponseDTO fromScheduledClass(ScheduledClass sc) {
        return ScheduledClassResponseDTO.builder()
                .id(sc.getId())
                .className(sc.getClassDefinition().getName())
                .instructorName(sc.getInstructor().getFullName())
                .clubName(sc.getClub().getName())
                .startTime(sc.getStartTime())
                .endTime(sc.getEndTime())
                .maxCapacity(sc.getMaxCapacity())
                .build();
    }
}