package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo hoặc cập nhật một lớp học được lên lịch")
public class ScheduledClassRequestDTO {

    @NotNull(message = "Vui lòng chọn loại lớp học")
    private Long classDefinitionId;

    @NotNull(message = "Vui lòng chọn HLV (Instructor)")
    private Long instructorId;

    @NotNull(message = "Vui lòng chọn chi nhánh")
    private Long clubId;

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    private OffsetDateTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    private OffsetDateTime endTime;

    @NotNull(message = "Số lượng tối đa là bắt buộc")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer maxCapacity;
}