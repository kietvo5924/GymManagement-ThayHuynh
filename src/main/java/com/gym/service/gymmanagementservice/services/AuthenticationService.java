package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.SignUpRequest;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public String signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        String verificationToken = jwtService.generateEmailVerificationToken(savedUser.getUsername());

        String verificationLink = "http://localhost:8080/api/auth/verify?token=" + verificationToken;
        String emailBody = String.format("""
            <h1>Cảm ơn bạn đã đăng ký tài khoản tại MyGym!</h1>
            <p>Vui lòng nhấp vào link sau để xác thực tài khoản của bạn (link có hiệu lực trong 15 phút):</p>
            <a href="%s" style="padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;">Xác thực tài khoản</a>
            """, verificationLink);

        emailService.sendVerificationEmail(savedUser.getEmail(), "Xác thực tài khoản", emailBody);
        return "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản.";
    }
}
