package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final CheckInLogRepository checkInLogRepository;
    private final ClubService clubService;
    private final ZoneId gymTimeZone = ZoneId.of("Asia/Ho_Chi_Minh");

    // HÀM KIỂM TRA KHUNG GIỜ (Giữ nguyên)
    private boolean isCheckInTimeValid(OffsetDateTime now, MemberPackage pkg) {
        LocalTime startTime = pkg.getGymPackage().getStartTimeLimit();
        LocalTime endTime = pkg.getGymPackage().getEndTimeLimit();
        if (startTime == null || endTime == null) {
            return true;
        }
        LocalTime localCheckInTime = now.atZoneSameInstant(gymTimeZone).toLocalTime();
        boolean isAfterOrEqualStart = !localCheckInTime.isBefore(startTime);
        boolean isBeforeOrEqualEnd = !localCheckInTime.isAfter(endTime);
        return isAfterOrEqualStart && isBeforeOrEqualEnd;
    }

    // HÀM KIỂM TRA TRUY CẬP CLB (Giữ nguyên)
    private boolean isClubAccessValid(MemberPackage pkg, Club currentClub) {
        GymPackage gymPackage = pkg.getGymPackage();

        if (gymPackage.getPackageType() == PackageType.PT_SESSION) {
            return true;
        }

        PackageAccessType accessType = gymPackage.getAccessType();
        Club targetClub = gymPackage.getTargetClub();

        if (accessType == PackageAccessType.ALL_CLUBS) {
            return true;
        }

        if (accessType == PackageAccessType.SINGLE_CLUB) {
            if (targetClub != null && targetClub.getId().equals(currentClub.getId())) {
                return true;
            }
        }
        return false;
    }


    @Transactional
    public CheckInResponseDTO performCheckIn(CheckInRequestDTO request) {
        OffsetDateTime now = OffsetDateTime.now();
        Club currentClub = clubService.getClubById(request.getClubId());
        Optional<Member> memberOpt = memberRepository.findByBarcode(request.getBarcode());

        // 2. Không tìm thấy hội viên
        if (memberOpt.isEmpty()) {
            createLog(null, currentClub, CheckInStatus.FAILED_MEMBER_NOT_FOUND, "Mã vạch không tồn tại.");

            CheckInResponseDTO response = new CheckInResponseDTO();
            response.setStatus(CheckInStatus.FAILED_MEMBER_NOT_FOUND);
            response.setMessage("Không tìm thấy hội viên!");
            response.setClubName(currentClub.getName());
            return response;
        }

        Member member = memberOpt.get();

        CheckInResponseDTO response = new CheckInResponseDTO();
        response.setMemberFullName(member.getFullName());
        response.setClubName(currentClub.getName());

        // 3. ƯU TIÊN 1: Kiểm tra gói GYM_ACCESS
        Optional<MemberPackage> activeGymAccessPackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                        member.getId(),
                        SubscriptionStatus.ACTIVE,
                        PackageType.GYM_ACCESS
                );

        if (activeGymAccessPackageOpt.isPresent()) {
            MemberPackage activePackage = activeGymAccessPackageOpt.get();

            if (activePackage.getEndDate().isBefore(now)) {
                log.warn("Gói GYM_ACCESS ID {} có status ACTIVE nhưng đã hết hạn.", activePackage.getId());
            } else {

                // KIỂM TRA MỚI: QUYỀN TRUY CẬP CLB
                if (!isClubAccessValid(activePackage, currentClub)) {
                    String errorMsg = String.format("Gói [%s] không hợp lệ tại chi nhánh [%s].",
                            activePackage.getGymPackage().getName(), currentClub.getName());
                    createLog(member, currentClub, CheckInStatus.FAILED_CLUB_MISMATCH, errorMsg);

                    response.setStatus(CheckInStatus.FAILED_CLUB_MISMATCH);
                    response.setMessage(errorMsg);
                    response.setPackageName(activePackage.getGymPackage().getName());
                    response.setPackageEndDate(activePackage.getEndDate());
                    return response;
                }

                // KIỂM TRA CŨ: KHUNG GIỜ
                if (!isCheckInTimeValid(now, activePackage)) {
                    String errorMsg = String.format("Gói tập [%s] chỉ hợp lệ trong khung giờ %s - %s.",
                            activePackage.getGymPackage().getName(),
                            activePackage.getGymPackage().getStartTimeLimit(),
                            activePackage.getGymPackage().getEndTimeLimit());
                    createLog(member, currentClub, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

                    response.setStatus(CheckInStatus.FAILED_OFF_PEAK_TIME);
                    response.setMessage(errorMsg);
                    response.setPackageName(activePackage.getGymPackage().getName());
                    response.setPackageEndDate(activePackage.getEndDate());
                    return response;
                }

                // Check-in thành công với gói GYM_ACCESS
                createLog(member, currentClub, CheckInStatus.SUCCESS, "Check-in thành công (Gói Gym Access).");

                response.setStatus(CheckInStatus.SUCCESS);
                response.setMessage("Check-in thành công!");
                response.setPackageName(activePackage.getGymPackage().getName());
                response.setPackageEndDate(activePackage.getEndDate());
                return response;
            }
        }

        // 4. ƯU TIÊN 2: Kiểm tra gói PER_VISIT
        Optional<MemberPackage> activePerVisitPackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeAndEndDateAfterAndRemainingSessionsGreaterThanOrderByEndDateAsc(
                        member.getId(),
                        SubscriptionStatus.ACTIVE,
                        PackageType.PER_VISIT,
                        now,
                        0
                );

        if (activePerVisitPackageOpt.isPresent()) {
            MemberPackage perVisitPackage = activePerVisitPackageOpt.get();

            // KIỂM TRA MỚI: QUYỀN TRUY CẬP CLB
            if (!isClubAccessValid(perVisitPackage, currentClub)) {
                String errorMsg = String.format("Gói [%s] không hợp lệ tại chi nhánh [%s].",
                        perVisitPackage.getGymPackage().getName(), currentClub.getName());
                createLog(member, currentClub, CheckInStatus.FAILED_CLUB_MISMATCH, errorMsg);

                response.setStatus(CheckInStatus.FAILED_CLUB_MISMATCH);
                response.setMessage(errorMsg);
                response.setPackageName(perVisitPackage.getGymPackage().getName());
                response.setPackageEndDate(perVisitPackage.getEndDate());
                return response;
            }

            // KIỂM TRA CŨ: KHUNG GIỜ
            if (!isCheckInTimeValid(now, perVisitPackage)) {
                String errorMsg = String.format("Gói tập [%s] chỉ hợp lệ trong khung giờ %s - %s.",
                        perVisitPackage.getGymPackage().getName(),
                        perVisitPackage.getGymPackage().getStartTimeLimit(),
                        perVisitPackage.getGymPackage().getEndTimeLimit());
                createLog(member, currentClub, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

                response.setStatus(CheckInStatus.FAILED_OFF_PEAK_TIME);
                response.setMessage(errorMsg);
                response.setPackageName(perVisitPackage.getGymPackage().getName());
                response.setPackageEndDate(perVisitPackage.getEndDate());
                return response;
            }

            // TRỪ 1 LƯỢT CHECK-IN
            int remaining = perVisitPackage.getRemainingSessions() - 1;
            perVisitPackage.setRemainingSessions(remaining);
            String message = String.format("Check-in thành công (Gói Per-Visit). Còn lại %d lượt.", remaining);
            if (remaining == 0) {
                perVisitPackage.setStatus(SubscriptionStatus.EXPIRED);
            }
            memberPackageRepository.save(perVisitPackage);
            createLog(member, currentClub, CheckInStatus.SUCCESS, message);

            response.setStatus(CheckInStatus.SUCCESS);
            response.setMessage(message);
            response.setPackageName(perVisitPackage.getGymPackage().getName());
            response.setPackageEndDate(perVisitPackage.getEndDate());
            return response;
        }

        // 5. KHÔNG TÌM THẤY GÓI NÀO HỢP LỆ
        createLog(member, currentClub, CheckInStatus.FAILED_NO_ACTIVE_PACKAGE, "Hội viên không có gói tập (Gym Access/Per-Visit) nào đang hoạt động.");

        response.setStatus(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE);
        response.setMessage("Hội viên không có gói tập nào đang hoạt động!");
        return response;
    }

    // Cập nhật hàm createLog
    private void createLog(Member member, Club club, CheckInStatus status, String message) {

        // Dùng constructor (AllArgsConstructor của CheckInLog là public)
        CheckInLog log = new CheckInLog(
                null,                 // id
                member,               // member
                OffsetDateTime.now(), // checkInTime
                status,               // status
                message               // message
        );

        checkInLogRepository.save(log);
    }
}