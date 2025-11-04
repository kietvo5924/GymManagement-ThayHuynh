package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.FreezeRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberRepository memberRepository;
    private final GymPackageRepository gymPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuthenticationService authenticationService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage gymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        MemberPackage.MemberPackageBuilder subscriptionBuilder = MemberPackage.builder()
                .member(member)
                .gymPackage(gymPackage)
                .status(SubscriptionStatus.ACTIVE);

        if (gymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            boolean hasActiveGymPackage = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                            member.getId(), SubscriptionStatus.ACTIVE, PackageType.GYM_ACCESS)
                    .isPresent();

            if (hasActiveGymPackage) {
                throw new IllegalStateException("Hội viên này đã có một gói tập GYM ACCESS đang hoạt động. Vui lòng sử dụng chức năng Gia hạn.");
            }

            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate = startDate.plusDays(gymPackage.getDurationDays());
            subscriptionBuilder.startDate(startDate).endDate(endDate);

        } else if (gymPackage.getPackageType() == PackageType.PT_SESSION) {
            subscriptionBuilder.remainingSessions(gymPackage.getNumberOfSessions());

            // --- LOGIC GÁN PT ĐÃ BỊ XÓA BỎ ---

        } else if (gymPackage.getPackageType() == PackageType.PER_VISIT) {
            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate = startDate.plusDays(gymPackage.getDurationDays());

            subscriptionBuilder.startDate(startDate)
                    .endDate(endDate)
                    .remainingSessions(gymPackage.getNumberOfSessions());
        }

        MemberPackage savedSubscription = memberPackageRepository.save(subscriptionBuilder.build());

        Transaction transaction = Transaction.builder()
                .amount(gymPackage.getPrice())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(savedSubscription)
                .sale(null)
                .build();

        transactionRepository.save(transaction);

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

    @Transactional
    public SubscriptionResponseDTO renewSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage newGymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {
            Optional<MemberPackage> existingActivePtPackageOpt = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_Id(
                            member.getId(),
                            SubscriptionStatus.ACTIVE,
                            newGymPackage.getId()
                    );

            if (existingActivePtPackageOpt.isPresent()) {
                MemberPackage packageToRenew = existingActivePtPackageOpt.get();
                int currentSessions = packageToRenew.getRemainingSessions() != null ? packageToRenew.getRemainingSessions() : 0;
                int newSessions = newGymPackage.getNumberOfSessions() != null ? newGymPackage.getNumberOfSessions() : 0;

                packageToRenew.setRemainingSessions(currentSessions + newSessions);

                // --- LOGIC GÁN PT ĐÃ BỊ XÓA BỎ ---

                if (packageToRenew.getStatus() == SubscriptionStatus.EXPIRED) {
                    packageToRenew.setStatus(SubscriptionStatus.ACTIVE);
                }

                MemberPackage savedSubscription = memberPackageRepository.save(packageToRenew);
                // (Chưa tạo transaction cho gia hạn, bạn nên thêm sau)
                return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
            }
        }

        MemberPackage.MemberPackageBuilder newSubscriptionBuilder = MemberPackage.builder()
                .member(member)
                .gymPackage(newGymPackage)
                .status(SubscriptionStatus.ACTIVE);

        if (newGymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            MemberPackage lastActivePackage = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                            member.getId(),
                            SubscriptionStatus.ACTIVE,
                            PackageType.GYM_ACCESS
                    )
                    .orElseThrow(() -> new IllegalStateException("Hội viên không có gói tập GYM ACCESS nào đang hoạt động để gia hạn."));

            OffsetDateTime newStartDate = lastActivePackage.getEndDate();
            OffsetDateTime newEndDate = newStartDate.plusDays(newGymPackage.getDurationDays());
            newSubscriptionBuilder.startDate(newStartDate).endDate(newEndDate);

        } else if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {
            newSubscriptionBuilder.remainingSessions(newGymPackage.getNumberOfSessions());
            // --- LOGIC GÁN PT ĐÃ BỊ XÓA BỎ ---
        }

        MemberPackage savedSubscription = memberPackageRepository.save(newSubscriptionBuilder.build());
        // (Chưa tạo transaction cho gia hạn, bạn nên thêm sau)
        return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
    }

    // (Các hàm Hủy, Đóng băng, Mở băng giữ nguyên)
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

    @Transactional
    public SubscriptionResponseDTO freezeSubscription(Long subscriptionId, FreezeRequestDTO request) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if(subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể đóng băng các gói tập đang hoạt động.");
        }
        subscription.setEndDate(subscription.getEndDate().plusDays(request.getFreezeDays()));
        subscription.setStatus(SubscriptionStatus.FROZEN);
        MemberPackage updatedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(updatedSubscription);
    }

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