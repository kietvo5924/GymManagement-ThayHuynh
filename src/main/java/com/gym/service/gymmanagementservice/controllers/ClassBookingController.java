package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.ClassBookingResponseDTO;
import com.gym.service.gymmanagementservice.dtos.ScheduledClassResponseDTO;
import com.gym.service.gymmanagementservice.services.ClassBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api") // Đặt base path là /api
@Tag(name = "Class Booking API (Member-Facing)", description = "API cho Hội viên (App) xem và đặt lịch lớp")
@SecurityRequirement(name = "bearerAuth") // Yêu cầu JWT
@PreAuthorize("hasRole('MEMBER')") // Chỉ MEMBER mới được dùng các API này
public class ClassBookingController {

    private final ClassBookingService classBookingService;

    /**
     * API 1: Lấy danh sách các lớp học có sẵn
     */
    @GetMapping("/classes")
    @Operation(summary = "Lấy danh sách lớp học có sẵn tại 1 CLB (cho App)")
    public ResponseEntity<List<ScheduledClassResponseDTO>> getAvailableClasses(
            @RequestParam Long clubId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {

        List<ScheduledClassResponseDTO> classes = classBookingService.getAvailableClasses(clubId, start, end);
        return ResponseEntity.ok(classes);
    }

    /**
     * API 2: Đặt 1 lớp học
     */
    @PostMapping("/classes/{scheduledClassId}/book")
    @Operation(summary = "Đặt 1 lớp học (cho App)")
    public ResponseEntity<ClassBookingResponseDTO> bookClass(@PathVariable Long scheduledClassId) {
        ClassBookingResponseDTO booking = classBookingService.bookClass(scheduledClassId);
        return ResponseEntity.ok(booking);
    }

    /**
     * API 3: Lấy danh sách các lớp tôi đã đặt
     */
    @GetMapping("/bookings/my-bookings")
    @Operation(summary = "Lấy danh sách các lớp tôi đã đặt (cho App)")
    public ResponseEntity<List<ClassBookingResponseDTO>> getMyBookings() {
        List<ClassBookingResponseDTO> bookings = classBookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    /**
     * API 4: Hủy 1 lớp đã đặt
     */
    @PutMapping("/bookings/{bookingId}/cancel") // Dùng PUT vì đây là hành động cập nhật trạng thái
    @Operation(summary = "Hủy 1 lớp đã đặt (cho App)")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        classBookingService.cancelBooking(bookingId);
        return ResponseEntity.ok("Đã hủy đặt chỗ thành công.");
    }
}