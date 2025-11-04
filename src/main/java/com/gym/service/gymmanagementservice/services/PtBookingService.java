package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PtBookingRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PtBookingResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PtBookingService {

    private final PtBookingRepository ptBookingRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AuthenticationService authenticationService;
    private final PtSessionService ptSessionService; // Dùng để trừ buổi

    // Lấy Member đang đăng nhập
    private Member getCurrentMember() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member member = currentUser.getMemberProfile();
        if (member == null) {
            throw new EntityNotFoundException("Không tìm thấy hồ sơ hội viên cho tài khoản này.");
        }
        return member;
    }

    // Lấy PT (User) đang đăng nhập
    private User getCurrentPt() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if (currentUser.getRole() != Role.PT) {
            throw new AccessDeniedException("Tài khoản này không phải là PT.");
        }
        return currentUser;
    }

    /**
     * API 1: (Member) Yêu cầu 1 buổi tập
     */
    @Transactional
    public PtBookingResponseDTO requestBooking(PtBookingRequestDTO request) {
        Member member = getCurrentMember();
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Kiểm tra các đối tượng
        MemberPackage ptPackage = memberPackageRepository.findById(request.getMemberPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói PT ID: " + request.getMemberPackageId()));
        User pt = userRepository.findById(request.getPtUserId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT ID: " + request.getPtUserId()));
        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CLB ID: " + request.getClubId()));

        // 2. Validate Gói tập
        if (!ptPackage.getMember().getId().equals(member.getId())) {
            throw new AccessDeniedException("Bạn không sở hữu gói PT này.");
        }
        if (ptPackage.getGymPackage().getPackageType() != PackageType.PT_SESSION) {
            throw new IllegalArgumentException("Gói này không phải là gói PT.");
        }
        if (ptPackage.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói PT này không hoạt động.");
        }
        if (ptPackage.getRemainingSessions() <= 0) {
            throw new IllegalStateException("Gói PT đã hết buổi.");
        }

        // 3. Validate PT
        if (pt.getRole() != Role.PT) {
            throw new IllegalArgumentException("Người dùng ID " + pt.getId() + " không phải là PT.");
        }

        // 4. Validate Thời gian
        if (request.getStartTime().isBefore(now.plusMinutes(30))) {
            throw new IllegalArgumentException("Thời gian đặt lịch phải sau ít nhất 30 phút kể từ bây giờ.");
        }
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc.");
        }

        // 5. KIỂM TRA QUAN TRỌNG: PT có rảnh không?

        // 5a. PT có đang làm việc vào giờ đó không?
        boolean isWorking = workScheduleRepository.existsByUserIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                pt.getId(), request.getStartTime(), request.getEndTime());
        if (!isWorking) {
            throw new IllegalStateException("PT không có lịch làm việc (WorkSchedule) vào khung giờ này.");
        }

        // 5b. PT có bị trùng lịch hẹn (booking) khác không?
        List<PtBooking> existingPtBookings = ptBookingRepository.findAllByPtUserIdAndStartTimeBetween(
                pt.getId(), now, request.getEndTime().plusDays(1));

        for (PtBooking booking : existingPtBookings) {
            if (booking.getStatus() == PtBookingStatus.CONFIRMED_BY_PT) {
                // Check overlap
                if (request.getStartTime().isBefore(booking.getEndTime()) && request.getEndTime().isAfter(booking.getStartTime())) {
                    throw new IllegalStateException("PT đã có lịch hẹn khác bị trùng vào khung giờ này.");
                }
            }
        }

        // 6. Tạo Booking
        PtBooking newBooking = PtBooking.builder()
                .memberPackage(ptPackage)
                .member(member)
                .ptUser(pt)
                .club(club)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(PtBookingStatus.REQUESTED_BY_MEMBER)
                .notesByMember(request.getNotesByMember())
                .build();

        PtBooking savedBooking = ptBookingRepository.save(newBooking);
        return PtBookingResponseDTO.fromPtBooking(savedBooking);
    }

    /**
     * API 2: (Member) Hủy 1 yêu cầu (booking)
     */
    @Transactional
    public void cancelMyBooking(Long bookingId) {
        Member member = getCurrentMember();
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch hẹn ID: " + bookingId));

        if (!booking.getMember().getId().equals(member.getId())) {
            throw new AccessDeniedException("Bạn không sở hữu lịch hẹn này.");
        }

        if (booking.getStatus() != PtBookingStatus.REQUESTED_BY_MEMBER && booking.getStatus() != PtBookingStatus.CONFIRMED_BY_PT) {
            throw new IllegalStateException("Bạn chỉ có thể hủy lịch hẹn đang 'Chờ xác nhận' hoặc 'Đã xác nhận'.");
        }

        if (booking.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Không thể hủy lịch hẹn đã qua.");
        }

        booking.setStatus(PtBookingStatus.CANCELED_BY_MEMBER);
        ptBookingRepository.save(booking);
    }

    /**
     * API 3: (Member) Xem các lịch hẹn của tôi
     */
    public List<PtBookingResponseDTO> getMyBookings() {
        Member member = getCurrentMember();
        return ptBookingRepository.findAllByMemberIdOrderByStartTimeDesc(member.getId())
                .stream()
                .map(PtBookingResponseDTO::fromPtBooking)
                .collect(Collectors.toList());
    }

    /**
     * API 4: (PT) Xem lịch hẹn của tôi
     */
    public List<PtBookingResponseDTO> getMyPtSchedule() {
        User pt = getCurrentPt();
        return ptBookingRepository.findAllByPtUserIdOrderByStartTimeDesc(pt.getId())
                .stream()
                .map(PtBookingResponseDTO::fromPtBooking)
                .collect(Collectors.toList());
    }

    /**
     * API 5: (PT) Xác nhận (Confirm) 1 yêu cầu
     */
    @Transactional
    public PtBookingResponseDTO confirmBooking(Long bookingId) {
        User pt = getCurrentPt();
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch hẹn ID: " + bookingId));

        if (!booking.getPtUser().getId().equals(pt.getId())) {
            throw new AccessDeniedException("Đây không phải là lịch hẹn của bạn.");
        }

        if (booking.getStatus() != PtBookingStatus.REQUESTED_BY_MEMBER) {
            throw new IllegalStateException("Bạn chỉ có thể xác nhận lịch hẹn đang 'Chờ'.");
        }

        booking.setStatus(PtBookingStatus.CONFIRMED_BY_PT);
        PtBooking savedBooking = ptBookingRepository.save(booking);
        return PtBookingResponseDTO.fromPtBooking(savedBooking);
    }

    /**
     * API 6: (PT) Hủy 1 yêu cầu
     */
    @Transactional
    public void cancelPtBooking(Long bookingId) {
        User pt = getCurrentPt();
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch hẹn ID: " + bookingId));

        if (!booking.getPtUser().getId().equals(pt.getId())) {
            throw new AccessDeniedException("Đây không phải là lịch hẹn của bạn.");
        }

        booking.setStatus(PtBookingStatus.CANCELED_BY_PT);
        ptBookingRepository.save(booking);
    }

    /**
     * API 7: (PT/Admin) Hoàn thành 1 buổi tập
     */
    @Transactional
    public PtBookingResponseDTO completeBooking(Long bookingId, String ptNotes) {
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch hẹn ID: " + bookingId));

        if (booking.getStatus() != PtBookingStatus.CONFIRMED_BY_PT) {
            throw new IllegalStateException("Chỉ có thể hoàn thành buổi tập đã 'Xác nhận'.");
        }

        if (booking.getEndTime().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Chưa đến giờ kết thúc buổi tập.");
        }

        // 1. Cập nhật trạng thái Booking
        booking.setStatus(PtBookingStatus.COMPLETED);
        booking.setNotesByPt(ptNotes);

        // 2. GỌI SERVICE CŨ ĐỂ TRỪ BUỔI TẬP
        try {
            // (Service này bây giờ chỉ dùng để trừ buổi)
            ptSessionService.logPtSession(booking.getMemberPackage().getId(), ptNotes);
        } catch (Exception e) {
            throw new IllegalStateException("Hoàn thành buổi tập thất bại: " + e.getMessage());
        }

        PtBooking savedBooking = ptBookingRepository.save(booking);
        return PtBookingResponseDTO.fromPtBooking(savedBooking);
    }

    // --- ĐÂY LÀ HÀM BỊ THIẾU MÀ BẠN CẦN ---
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

    /**
     * API 9: (Admin) Lấy TẤT CẢ các lịch hẹn (để quản lý)
     */
    public List<PtBookingResponseDTO> getAllBookings() {
        // Sắp xếp theo cái mới nhất
        return ptBookingRepository.findAll().stream()
                .sorted((b1, b2) -> b2.getStartTime().compareTo(b1.getStartTime()))
                .map(PtBookingResponseDTO::fromPtBooking)
                .collect(Collectors.toList());
    }
}