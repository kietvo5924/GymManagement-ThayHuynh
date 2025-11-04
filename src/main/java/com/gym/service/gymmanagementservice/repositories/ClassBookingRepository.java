package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.BookingStatus;
import com.gym.service.gymmanagementservice.models.ClassBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassBookingRepository extends JpaRepository<ClassBooking, Long> {

    // Tìm các lượt đặt chỗ của 1 hội viên
    List<ClassBooking> findAllByMemberId(Long memberId);

    // Tìm xem hội viên đã đặt lớp này chưa
    Optional<ClassBooking> findByMemberIdAndScheduledClassId(Long memberId, Long scheduledClassId);

    // Đếm số lượng người đã đặt (để kiểm tra max_capacity)
    int countByScheduledClassIdAndStatus(Long scheduledClassId, BookingStatus status);

    // Lấy danh sách người đã book 1 lớp
    List<ClassBooking> findAllByScheduledClassIdAndStatus(Long scheduledClassId, BookingStatus status);

    boolean existsByScheduledClassId(Long scheduledClassId);

    List<ClassBooking> findAllByMemberIdOrderByScheduledClassStartTimeAsc(Long memberId);
}