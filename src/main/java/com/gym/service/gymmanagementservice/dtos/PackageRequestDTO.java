package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo hoặc cập nhật gói tập")
public class PackageRequestDTO {

    @Schema(description = "Tên gói tập", example = "Gói tập 1 tháng")
    @NotBlank(message = "Tên gói tập là bắt buộc")
    private String name;

    @Schema(description = "Mô tả chi tiết về gói tập", example = "Gói tập không giới hạn cho mọi khung giờ")
    private String description;

    @Schema(description = "Giá của gói tập", example = "500000.00")
    @NotNull(message = "Giá là bắt buộc")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Schema(description = "Thời hạn của gói tập (số ngày)", example = "30")
    @NotNull(message = "Thời hạn là bắt buộc")
    @Min(value = 1, message = "Thời hạn phải ít nhất là 1 ngày")
    private Integer durationDays;
}
