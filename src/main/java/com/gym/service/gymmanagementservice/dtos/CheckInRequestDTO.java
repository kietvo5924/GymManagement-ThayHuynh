package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // <-- IMPORT MỚI
import lombok.Data;

@Data
public class CheckInRequestDTO {
    @NotBlank(message = "Mã vạch là bắt buộc")
    @Schema(description = "Mã vạch/QR code được quét từ thẻ của hội viên")
    private String barcode;

    // TRƯỜNG MỚI
    @NotNull(message = "Vui lòng chọn chi nhánh (CLB) bạn đang làm việc")
    @Schema(description = "ID của Chi nhánh (Club) nơi đang thực hiện check-in")
    private Long clubId;
}