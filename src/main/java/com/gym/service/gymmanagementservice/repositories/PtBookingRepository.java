package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PtBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PtBookingRepository extends JpaRepository<PtBooking, Long> {

    // Tìm các lịch hẹn của 1 PT trong 1 khoảng thời gian
    List<PtBooking> findAllByPtUserIdAndStartTimeBetween(Long ptUserId, OffsetDateTime start, OffsetDateTime end);

    // Tìm các lịch hẹn của 1 Hội viên
    List<PtBooking> findAllByMemberIdOrderByStartTimeDesc(Long memberId);

    // Tìm các lịch hẹn của 1 PT
    List<PtBooking> findAllByPtUserIdOrderByStartTimeDesc(Long ptUserId);
}