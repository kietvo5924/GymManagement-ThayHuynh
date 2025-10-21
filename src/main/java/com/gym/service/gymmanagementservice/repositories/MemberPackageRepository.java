package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.MemberPackage;
import com.gym.service.gymmanagementservice.models.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberPackageRepository extends JpaRepository<MemberPackage, Long> {
    // Tìm tất cả các gói đã đăng ký của một hội viên
    List<MemberPackage> findByMemberId(Long memberId);
    boolean existsByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

    // Tìm gói tập đầu tiên của hội viên theo status, sắp xếp theo ngày hết hạn mới nhất
    Optional<MemberPackage> findFirstByMemberIdAndStatusOrderByEndDateDesc(Long memberId, SubscriptionStatus status);
}