package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.FreezeRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.GymPackageRepository;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberRepository memberRepository;
    private final GymPackageRepository gymPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuthenticationService authenticationService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage gymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        boolean hasActivePackage = memberPackageRepository.existsByMemberIdAndStatus(request.getMemberId(), SubscriptionStatus.ACTIVE);
        if (hasActivePackage) {
            throw new IllegalStateException("Hội viên này đã có một gói tập đang hoạt động. Vui lòng sử dụng chức năng Gia hạn.");
        }

        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.plusDays(gymPackage.getDurationDays());

        MemberPackage subscription = MemberPackage.builder()
                .member(member)
                .gymPackage(gymPackage)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        MemberPackage savedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
    }

    public List<SubscriptionResponseDTO> getSubscriptionsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId);
        }

        return memberPackageRepository.findByMemberId(memberId).stream()
                .map(SubscriptionResponseDTO::fromMemberPackage)
                .collect(Collectors.toList());
    }

    // Gia hạn gói tập
    @Transactional
    public SubscriptionResponseDTO renewSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage newGymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        // Tìm gói tập đang ACTIVE gần nhất để tính ngày bắt đầu cho gói mới
        MemberPackage lastActivePackage = memberPackageRepository
                .findFirstByMemberIdAndStatusOrderByEndDateDesc(member.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Hội viên không có gói tập nào đang hoạt động để gia hạn."));

        // Ngày bắt đầu của gói mới là ngày kết thúc của gói cũ
        OffsetDateTime newStartDate = lastActivePackage.getEndDate();
        OffsetDateTime newEndDate = newStartDate.plusDays(newGymPackage.getDurationDays());

        MemberPackage newSubscription = MemberPackage.builder()
                .member(member)
                .gymPackage(newGymPackage)
                .startDate(newStartDate)
                .endDate(newEndDate)
                .status(SubscriptionStatus.ACTIVE) // Gói gia hạn cũng được kích hoạt ngay
                .build();

        MemberPackage savedSubscription = memberPackageRepository.save(newSubscription);
        return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
    }

    // Hủy gói tập
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể hủy các gói tập đang hoạt động.");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        memberPackageRepository.save(subscription);
    }

    // Đóng băng gói tập
    @Transactional
    public SubscriptionResponseDTO freezeSubscription(Long subscriptionId, FreezeRequestDTO request) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể đóng băng các gói tập đang hoạt động.");
        }

        // Cộng thêm số ngày đóng băng vào ngày kết thúc
        subscription.setEndDate(subscription.getEndDate().plusDays(request.getFreezeDays()));
        subscription.setStatus(SubscriptionStatus.FROZEN);

        MemberPackage updatedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(updatedSubscription);
    }

    // Mở lại gói tập đã đóng băng
    @Transactional
    public SubscriptionResponseDTO unfreezeSubscription(Long subscriptionId) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.FROZEN) {
            throw new IllegalStateException("Gói tập này không ở trạng thái đóng băng.");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        MemberPackage updatedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(updatedSubscription);
    }
}