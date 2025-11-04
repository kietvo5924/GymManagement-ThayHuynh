package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.PtBookingRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PtBookingResponseDTO;
import com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO; // Dùng để nhận notes
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.PtBookingRepository;
import com.gym.service.gymmanagementservice.services.PtBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pt-bookings")
@Tag(name = "PT Booking API (Member & PT)", description = "API cho Hội viên và PT quản lý lịch hẹn")
@SecurityRequirement(name = "bearerAuth")
public class PtBookingController {

    private final PtBookingService ptBookingService;
    private final MemberRepository memberRepository;
    private final PtBookingRepository ptBookingRepository;

    // === API CHO HỘI VIÊN (MEMBER) ===

    @PostMapping("/request")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên yêu cầu (đặt) 1 buổi tập PT")
    public ResponseEntity<PtBookingResponseDTO> requestBooking(@Valid @RequestBody PtBookingRequestDTO request) {
        PtBookingResponseDTO booking = ptBookingService.requestBooking(request);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên xem lịch sử đặt PT của mình")
    public ResponseEntity<List<PtBookingResponseDTO>> getMyBookings() {
        List<PtBookingResponseDTO> bookings = ptBookingService.getMyBookings();
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{bookingId}/cancel-member")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên tự hủy lịch hẹn")
    public ResponseEntity<String> cancelMyBooking(@PathVariable Long bookingId) {
        ptBookingService.cancelMyBooking(bookingId);
        return ResponseEntity.ok("Đã hủy lịch hẹn.");
    }

    // === API CHO HUẤN LUYỆN VIÊN (PT) ===

    @GetMapping("/my-schedule")
    @PreAuthorize("hasRole('PT')")
    @Operation(summary = "PT xem lịch hẹn (schedule) của mình")
    public ResponseEntity<List<PtBookingResponseDTO>> getMyPtSchedule() {
        List<PtBookingResponseDTO> schedule = ptBookingService.getMyPtSchedule();
        return ResponseEntity.ok(schedule);
    }

    /**
     * API 8: (Admin/Staff) Xem lịch sử đặt PT của 1 hội viên cụ thể
     */
    public List<PtBookingResponseDTO> getBookingsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Không tìm thấy hội viên ID: " + memberId);
        }
        return ptBookingRepository.findAllByMemberIdOrderByStartTimeDesc(memberId)
                .stream()
                .map(PtBookingResponseDTO::fromPtBooking)
                .collect(Collectors.toList());
    }

    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('PT')")
    @Operation(summary = "PT xác nhận (confirm) 1 yêu cầu từ hội viên")
    public ResponseEntity<PtBookingResponseDTO> confirmBooking(@PathVariable Long bookingId) {
        PtBookingResponseDTO booking = ptBookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{bookingId}/cancel-pt")
    @PreAuthorize("hasRole('PT')")
    @Operation(summary = "PT hủy 1 lịch hẹn (đã confirm hoặc đang request)")
    public ResponseEntity<String> cancelPtBooking(@PathVariable Long bookingId) {
        ptBookingService.cancelPtBooking(bookingId);
        return ResponseEntity.ok("PT đã hủy lịch hẹn.");
    }

    // === API CHO PT HOẶC ADMIN (HOÀN THÀNH BUỔI TẬP) ===

    @PutMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyRole('PT', 'ADMIN')")
    @Operation(summary = "PT (hoặc Admin) xác nhận buổi tập đã HOÀN THÀNH (sẽ trừ buổi)")
    public ResponseEntity<PtBookingResponseDTO> completeBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) PtLogRequestDTO notesRequest) {

        String notes = (notesRequest != null) ? notesRequest.getNotes() : "Hoàn thành buổi tập.";
        PtBookingResponseDTO booking = ptBookingService.completeBooking(bookingId, notes);
        return ResponseEntity.ok(booking);
    }
}