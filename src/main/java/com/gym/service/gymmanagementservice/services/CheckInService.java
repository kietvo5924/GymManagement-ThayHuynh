package com.gym.service.gymmanagementservice.services;

package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final CheckInLogRepository checkInLogRepository;

    @Transactional
    public CheckInResponseDTO performCheckIn(CheckInRequestDTO request) {
        Optional<Member> memberOpt = memberRepository.findByBarcode(request.getBarcode());

        // Không tìm thấy hội viên
        if (memberOpt.isEmpty()) {
            createLog(null, CheckInStatus.FAILED_MEMBER_NOT_FOUND, "Mã vạch không tồn tại trong hệ thống.");
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_MEMBER_NOT_FOUND)
                    .message("Không tìm thấy hội viên!")
                    .build();
        }

        Member member = memberOpt.get();
        Optional<MemberPackage> activePackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusOrderByEndDateDesc(member.getId(), SubscriptionStatus.ACTIVE);

        // Hội viên không có gói tập nào đang hoạt động
        if (activePackageOpt.isEmpty()) {
            createLog(member, CheckInStatus.FAILED_NO_ACTIVE_PACKAGE, "Hội viên không có gói tập nào đang hoạt động.");
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                    .message("Hội viên không có gói tập nào đang hoạt động!")
                    .memberFullName(member.getFullName())
                    .build();
        }

        // Check-in thành công
        MemberPackage activePackage = activePackageOpt.get();
        createLog(member, CheckInStatus.SUCCESS, "Check-in thành công.");
        return CheckInResponseDTO.builder()
                .status(CheckInStatus.SUCCESS)
                .message("Check-in thành công!")
                .memberFullName(member.getFullName())
                .packageName(activePackage.getGymPackage().getName())
                .packageEndDate(activePackage.getEndDate())
                .build();
    }

    private void createLog(Member member, CheckInStatus status, String message) {
        CheckInLog log = CheckInLog.builder()
                .member(member)
                .checkInTime(OffsetDateTime.now())
                .status(status)
                .message(message)
                .build();
        checkInLogRepository.save(log);
    }
}
