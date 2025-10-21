package com.gym.service.gymmanagementservice.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class SaleRequestDTO {
    // ID của hội viên, có thể null
    private Long memberId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<SaleItemDTO> items;
}