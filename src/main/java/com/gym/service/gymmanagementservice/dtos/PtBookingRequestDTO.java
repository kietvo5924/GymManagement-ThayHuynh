package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Đối tượng yêu cầu (từ Member) để đặt lịch hẹn với PT")
public class PtBookingRequestDTO {

    @NotNull(message = "ID gói PT (MemberPackage) là bắt buộc")
    private Long memberPackageId;

    @NotNull(message = "ID của PT là bắt buộc")
    private Long ptUserId;

    @NotNull(message = "ID của Chi nhánh (Club) là bắt buộc")
    private Long clubId;

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    private OffsetDateTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    private OffsetDateTime endTime;

    @Schema(description = "Ghi chú của hội viên, vd: 'Hôm nay muốn tập ngực'")
    private String notesByMember;
}