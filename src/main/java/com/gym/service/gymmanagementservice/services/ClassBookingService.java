package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.ClassBookingResponseDTO;
import com.gym.service.gymmanagementservice.dtos.ScheduledClassResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.ClassBookingRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.ScheduledClassRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassBookingService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final ClassBookingRepository classBookingRepository;
    private final MemberRepository memberRepository;
    private final AuthenticationService authenticationService;

    // Lấy Member đang đăng nhập
    private Member getCurrentMember() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member member = currentUser.getMemberProfile();
        if (member == null) {
            throw new EntityNotFoundException("Không tìm thấy hồ sơ hội viên cho tài khoản này.");
        }
        return member;
    }

    /**
     * API 1: Lấy danh sách các lớp học có sẵn tại 1 CLB
     */
    public List<ScheduledClassResponseDTO> getAvailableClasses(Long clubId, OffsetDateTime start, OffsetDateTime end) {
        // Chỉ tìm các lớp trong tương lai
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime effectiveStart = (start.isBefore(now)) ? now : start;

        return scheduledClassRepository
                .findAllByClubIdAndStartTimeBetweenOrderByStartTimeAsc(clubId, effectiveStart, end)
                .stream()
                .map(ScheduledClassResponseDTO::fromScheduledClass)
                // (Sau này có thể thêm logic lọc các lớp đã đầy)
                .collect(Collectors.toList());
    }

    /**
     * API 2: Hội viên đặt 1 lớp học
     */
    @Transactional
    public ClassBookingResponseDTO bookClass(Long scheduledClassId) {
        Member member = getCurrentMember();
        ScheduledClass scheduledClass = scheduledClassRepository.findById(scheduledClassId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lớp học ID: " + scheduledClassId));

        // 1. Kiểm tra lớp đã qua chưa
        if (scheduledClass.getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Lớp học này đã diễn ra.");
        }

        // 2. Kiểm tra xem đã đặt lớp này chưa
        Optional<ClassBooking> existingBooking = classBookingRepository
                .findByMemberIdAndScheduledClassId(member.getId(), scheduledClassId);

        if (existingBooking.isPresent() && existingBooking.get().getStatus() == BookingStatus.BOOKED) {
            throw new IllegalStateException("Bạn đã đặt lớp này rồi.");
        }

        // (Nếu booking cũ là CANCELED, cho phép đặt lại)
        if (existingBooking.isPresent() && existingBooking.get().getStatus() == BookingStatus.CANCELED) {
            // Tái kích hoạt booking
            ClassBooking booking = existingBooking.get();
            booking.setStatus(BookingStatus.BOOKED);
            booking.setBookingTime(OffsetDateTime.now());
            ClassBooking savedBooking = classBookingRepository.save(booking);
            return ClassBookingResponseDTO.fromClassBooking(savedBooking);
        }


        // 3. Kiểm tra số lượng (Capacity)
        int currentBookings = classBookingRepository
                .countByScheduledClassIdAndStatus(scheduledClassId, BookingStatus.BOOKED);
        if (currentBookings >= scheduledClass.getMaxCapacity()) {
            throw new IllegalStateException("Lớp học đã đầy, vui lòng chọn lớp khác.");
        }

        // 4. Tạo booking mới
        ClassBooking newBooking = ClassBooking.builder()
                .member(member)
                .scheduledClass(scheduledClass)
                // (status và bookingTime được set tự động bởi @PrePersist)
                .build();

        ClassBooking savedBooking = classBookingRepository.save(newBooking);
        return ClassBookingResponseDTO.fromClassBooking(savedBooking);
    }

    /**
     * API 3: Hội viên hủy 1 lớp đã đặt
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Member member = getCurrentMember();
        ClassBooking booking = classBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lượt đặt chỗ ID: " + bookingId));

        // 1. Xác thực: Đúng là hội viên này đặt không?
        if (!booking.getMember().getId().equals(member.getId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy lượt đặt chỗ này.");
        }

        // 2. Kiểm tra lớp đã qua chưa (không cho hủy lớp đã qua)
        if (booking.getScheduledClass().getStartTime().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Không thể hủy lớp đã diễn ra.");
        }

        // 3. Hủy
        if (booking.getStatus() == BookingStatus.BOOKED) {
            booking.setStatus(BookingStatus.CANCELED);
            classBookingRepository.save(booking);
        } else {
            throw new IllegalStateException("Lượt đặt chỗ không ở trạng thái 'Đã đặt'.");
        }
    }

    /**
     * API 4: Lấy danh sách các lớp hội viên đã đặt
     */
    public List<ClassBookingResponseDTO> getMyBookings() {
        Member member = getCurrentMember();
        return classBookingRepository
                .findAllByMemberIdOrderByScheduledClassStartTimeAsc(member.getId())
                .stream()
                .map(ClassBookingResponseDTO::fromClassBooking)
                .collect(Collectors.toList());
    }
}