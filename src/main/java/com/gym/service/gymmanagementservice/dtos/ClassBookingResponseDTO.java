package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.BookingStatus;
import com.gym.service.gymmanagementservice.models.ClassBooking;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ClassBookingResponseDTO {
    private Long bookingId;
    private Long scheduledClassId;
    private String className;
    private String instructorName;
    private String clubName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private BookingStatus status;
    private OffsetDateTime bookingTime; // Thời điểm họ đặt

    public static ClassBookingResponseDTO fromClassBooking(ClassBooking booking) {
        return ClassBookingResponseDTO.builder()
                .bookingId(booking.getId())
                .scheduledClassId(booking.getScheduledClass().getId())
                .className(booking.getScheduledClass().getClassDefinition().getName())
                .instructorName(booking.getScheduledClass().getInstructor().getFullName())
                .clubName(booking.getScheduledClass().getClub().getName())
                .startTime(booking.getScheduledClass().getStartTime())
                .endTime(booking.getScheduledClass().getEndTime())
                .status(booking.getStatus())
                .bookingTime(booking.getBookingTime())
                .build();
    }
}