package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.ScheduledClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ScheduledClassRepository extends JpaRepository<ScheduledClass, Long> {

    // Tìm tất cả các lớp học trong 1 khoảng thời gian
    List<ScheduledClass> findAllByStartTimeBetween(OffsetDateTime start, OffsetDateTime end);

    // Tìm tất cả các lớp học tại 1 CLB trong 1 khoảng thời gian
    List<ScheduledClass> findAllByClubIdAndStartTimeBetween(Long clubId, OffsetDateTime start, OffsetDateTime end);

    List<ScheduledClass> findAllByClubIdAndStartTimeBetweenOrderByStartTimeAsc(Long clubId, OffsetDateTime start, OffsetDateTime end);
}