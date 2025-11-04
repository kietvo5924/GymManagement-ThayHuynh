package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PtBooking;
import com.gym.service.gymmanagementservice.models.PtBookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PtBookingResponseDTO {
    private Long bookingId;
    private Long memberPackageId;

    private Long memberId;
    private String memberName;

    private Long ptUserId;
    private String ptName;

    private Long clubId;
    private String clubName;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private PtBookingStatus status;
    private String notesByMember;
    private String notesByPt;

    public static PtBookingResponseDTO fromPtBooking(PtBooking booking) {
        return PtBookingResponseDTO.builder()
                .bookingId(booking.getId())
                .memberPackageId(booking.getMemberPackage().getId())
                .memberId(booking.getMember().getId())
                .memberName(booking.getMember().getFullName())
                .ptUserId(booking.getPtUser().getId())
                .ptName(booking.getPtUser().getFullName())
                .clubId(booking.getClub().getId())
                .clubName(booking.getClub().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .notesByMember(booking.getNotesByMember())
                .notesByPt(booking.getNotesByPt())
                .build();
    }
}