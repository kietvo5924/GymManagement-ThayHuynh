package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class SaleRequestDTO {
    private Long memberId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<SaleItemDTO> items;

    @NotNull(message = "Hình thức thanh toán là bắt buộc")
    private PaymentMethod paymentMethod;
}