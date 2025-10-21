package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng ký gói tập cho hội viên")
public class SubscriptionRequestDTO {
    @NotNull
    @Schema(description = "ID của hội viên")
    private Long memberId;

    @NotNull
    @Schema(description = "ID của gói tập (GymPackage)")
    private Long packageId;

    @NotNull(message = "Hình thức thanh toán là bắt buộc")
    private PaymentMethod paymentMethod;
}