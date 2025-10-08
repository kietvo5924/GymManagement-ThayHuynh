package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng ký tài khoản mới")
public class SignUpRequest {

    @Schema(description = "Họ và tên đầy đủ của người dùng", example = "Nguyễn Văn A")
    @NotBlank(message = "Họ và tên là bắt buộc")
    private String fullName;

    @Schema(description = "Số điện thoại của người dùng (đúng 10 chữ số)", example = "0987654321")
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 chữ số")
    private String phoneNumber;

    @Schema(description = "Địa chỉ email của người dùng (dùng để đăng nhập)", example = "nguyenvana@example.com")
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @Schema(description = "Mật khẩu (ít nhất 6 ký tự)", example = "password123")
    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}