package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng nhập")
public class SignInRequest {

    @Schema(description = "Email đã đăng ký", example = "nguyenvana@example.com")
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Schema(description = "Mật khẩu đã đăng ký", example = "password123")
    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}

