package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    // Tự động tìm tất cả CLB đang hoạt động
    List<Club> findAllByIsActive(boolean isActive);
}