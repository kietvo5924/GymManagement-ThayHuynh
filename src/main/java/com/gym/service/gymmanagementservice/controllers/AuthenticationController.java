package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.JwtAuthenticationResponse;
import com.gym.service.gymmanagementservice.dtos.SignInRequest;
import com.gym.service.gymmanagementservice.dtos.SignUpRequest;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Các API về Đăng ký, Đăng nhập và Xác thực")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Đăng ký tài khoản mới", description = "API cho phép người dùng mới đăng ký. Tài khoản sau khi tạo sẽ chưa được kích hoạt và cần xác thực qua email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công, yêu cầu kiểm tra email"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc Email đã tồn tại")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignUpRequest request) {
        String message = authenticationService.signup(request);
        return ResponseEntity.ok(message);
    }

    @Operation(summary = "Đăng nhập vào hệ thống", description = "Cung cấp email và mật khẩu để nhận về JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về token"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Sai email/mật khẩu hoặc tài khoản chưa được kích hoạt/bị khóa")
    })
    @PostMapping("/signin")
    public ResponseEntity<JwtAuthenticationResponse> signin(@Valid @RequestBody SignInRequest request) {
        JwtAuthenticationResponse response = authenticationService.signin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xác thực tài khoản qua email", description = "Người dùng sẽ được điều hướng đến đây từ link trong email để kích hoạt tài khoản.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xác thực thành công"),
            @ApiResponse(responseCode = "400", description = "Token không hợp lệ hoặc đã hết hạn")
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        try {
            String message = authenticationService.verifyAccount(token);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
