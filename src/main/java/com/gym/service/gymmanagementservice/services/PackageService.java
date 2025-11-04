package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.AmenityRepository;
import com.gym.service.gymmanagementservice.repositories.ClubRepository;
import com.gym.service.gymmanagementservice.repositories.GymPackageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final GymPackageRepository gymPackageRepository;
    private final ClubRepository clubRepository;
    private final AmenityRepository amenityRepository;

    @Transactional
    public PackageResponseDTO createPackage(PackageRequestDTO request) {
        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
        });

        validatePackageRequest(request);

        Club targetClub = resolveClub(request.getAccessType(), request.getTargetClubId());
        Set<Amenity> amenities = resolveAmenities(request.getAmenityIds());

        GymPackage newGymPackage = GymPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .packageType(request.getPackageType())
                .durationDays(request.getDurationDays())
                .numberOfSessions(request.getNumberOfSessions())
                .startTimeLimit(request.getStartTimeLimit())
                .endTimeLimit(request.getEndTimeLimit())
                .isActive(true)
                .accessType(request.getAccessType())
                .targetClub(targetClub)
                .amenities(amenities)
                .build();

        // Chuẩn hóa dữ liệu null dựa trên loại gói
        standardizePackage(newGymPackage);

        GymPackage savedGymPackage = gymPackageRepository.save(newGymPackage);
        return PackageResponseDTO.fromPackage(savedGymPackage);
    }

    public List<PackageResponseDTO> getAllPackages() {
        return gymPackageRepository.findAll().stream()
                .map(PackageResponseDTO::fromPackage)
                .collect(Collectors.toList());
    }

    public PackageResponseDTO getPackageById(Long id) {
        GymPackage pkg = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        return PackageResponseDTO.fromPackage(pkg);
    }

    @Transactional
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
            }
        });

        validatePackageRequest(request);

        Club targetClub = resolveClub(request.getAccessType(), request.getTargetClubId());
        Set<Amenity> amenities = resolveAmenities(request.getAmenityIds());

        existingGymPackage.setName(request.getName());
        existingGymPackage.setDescription(request.getDescription());
        existingGymPackage.setPrice(request.getPrice());
        existingGymPackage.setPackageType(request.getPackageType());
        existingGymPackage.setDurationDays(request.getDurationDays());
        existingGymPackage.setNumberOfSessions(request.getNumberOfSessions());
        existingGymPackage.setStartTimeLimit(request.getStartTimeLimit());
        existingGymPackage.setEndTimeLimit(request.getEndTimeLimit());
        existingGymPackage.setAccessType(request.getAccessType());
        existingGymPackage.setTargetClub(targetClub);
        existingGymPackage.setAmenities(amenities);

        // Chuẩn hóa dữ liệu null dựa trên loại gói
        standardizePackage(existingGymPackage);

        GymPackage updatedGymPackage = gymPackageRepository.save(existingGymPackage);
        return PackageResponseDTO.fromPackage(updatedGymPackage);
    }

    @Transactional
    public void togglePackageStatus(Long id) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        existingGymPackage.setActive(!existingGymPackage.isActive());
        gymPackageRepository.save(existingGymPackage);
    }

    // === CÁC HÀM HỖ TRỢ MỚI ===

    // Chuẩn hóa gói
    private void standardizePackage(GymPackage pkg) {
        // Chuẩn hóa logic cũ
        if (pkg.getPackageType() == PackageType.GYM_ACCESS) {
            pkg.setNumberOfSessions(null);
        } else if (pkg.getPackageType() == PackageType.PT_SESSION) {
            pkg.setDurationDays(null);
            pkg.setStartTimeLimit(null);
            pkg.setEndTimeLimit(null);
        }

        // Chuẩn hóa logic mới (AccessType)
        if (pkg.getAccessType() == PackageAccessType.ALL_CLUBS) {
            pkg.setTargetClub(null); // Gói tất cả CLB thì không cần targetClub
        }

        // Gói PT không áp dụng quyền truy cập CLB (logic này có thể thay đổi)
        if (pkg.getPackageType() == PackageType.PT_SESSION) {
            pkg.setAccessType(null);
            pkg.setTargetClub(null);
        }
    }

    // Tìm Club từ ID
    private Club resolveClub(PackageAccessType accessType, Long clubId) {
        if (accessType == PackageAccessType.SINGLE_CLUB) {
            if (clubId == null) {
                throw new IllegalArgumentException("Phải chọn một CLB (Target Club) khi Quyền truy cập là SINGLE_CLUB.");
            }
            return clubRepository.findById(clubId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CLB với ID: " + clubId));
        }
        return null; // Trả về null nếu là ALL_CLUBS
    }

    // Tìm danh sách Tiện ích từ Set<ID>
    private Set<Amenity> resolveAmenities(Set<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return new HashSet<>(); // Trả về Set rỗng
        }
        Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(amenityIds));
        if (amenities.size() != amenityIds.size()) {
            throw new EntityNotFoundException("Một hoặc nhiều ID tiện ích (Amenity) không hợp lệ.");
        }
        return amenities;
    }

    // Thêm hàm validate logic gói
    private void validatePackageRequest(PackageRequestDTO request) {

        LocalTime startTime = request.getStartTimeLimit();
        LocalTime endTime = request.getEndTimeLimit();

        // Kiểm tra logic khung giờ
        if (startTime != null && endTime != null) {
            if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                throw new IllegalArgumentException("Khung giờ bắt đầu phải sớm hơn khung giờ kết thúc.");
            }
        } else if (startTime != null || endTime != null) {
            throw new IllegalArgumentException("Phải cung cấp cả giờ bắt đầu và giờ kết thúc, hoặc để trống cả hai.");
        }

        if (request.getPackageType() != PackageType.PT_SESSION) {
            if (request.getAccessType() == null) {
                throw new IllegalArgumentException("Phải chọn Quyền truy cập (Access Type).");
            }
            if (request.getAccessType() == PackageAccessType.SINGLE_CLUB && request.getTargetClubId() == null) {
                throw new IllegalArgumentException("Phải chọn CLB (Target Club) khi Quyền truy cập là SINGLE_CLUB.");
            }
            if (request.getAccessType() == PackageAccessType.ALL_CLUBS && request.getTargetClubId() != null) {
                throw new IllegalArgumentException("Không được chọn CLB (Target Club) khi Quyền truy cập là ALL_CLUBS.");
            }
        }

        switch (request.getPackageType()) {
            case GYM_ACCESS:
                if (request.getDurationDays() == null || request.getDurationDays() <= 0) {
                    throw new IllegalArgumentException("Gói GYM_ACCESS phải có thời hạn (số ngày) lớn hơn 0.");
                }
                if (request.getNumberOfSessions() != null) {
                    throw new IllegalArgumentException("Gói GYM_ACCESS không yêu cầu số buổi (numberOfSessions).");
                }
                break;
            case PT_SESSION:
                if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
                    throw new IllegalArgumentException("Gói PT_SESSION phải có số buổi (numberOfSessions) lớn hơn 0.");
                }
                if (request.getDurationDays() != null) {
                    throw new IllegalArgumentException("Gói PT_SESSION (hiện tại) không yêu cầu thời hạn (durationDays).");
                }
                if (startTime != null || endTime != null) {
                    throw new IllegalArgumentException("Không thể áp dụng giới hạn giờ (Off-Peak) cho gói PT_SESSION.");
                }
                break;
            case PER_VISIT:
                if (request.getDurationDays() == null || request.getDurationDays() <= 0) {
                    throw new IllegalArgumentException("Gói PER_VISIT phải có thời hạn (số ngày) lớn hơn 0.");
                }
                if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
                    throw new IllegalArgumentException("Gói PER_VISIT phải có số lượt (numberOfSessions) lớn hơn 0.");
                }
                break;
            default:
                throw new IllegalArgumentException("Loại gói tập không xác định.");
        }
    }
}
