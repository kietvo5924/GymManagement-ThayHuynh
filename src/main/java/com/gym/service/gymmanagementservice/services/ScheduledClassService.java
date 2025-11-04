package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.ScheduledClassRequestDTO;
import com.gym.service.gymmanagementservice.dtos.ScheduledClassResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledClassService {

    private final ScheduledClassRepository scheduledClassRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClassBookingRepository classBookingRepository;

    public List<ScheduledClassResponseDTO> getAll(OffsetDateTime start, OffsetDateTime end) {
        return scheduledClassRepository.findAllByStartTimeBetween(start, end).stream()
                .map(ScheduledClassResponseDTO::fromScheduledClass)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduledClassResponseDTO create(ScheduledClassRequestDTO request) {
        // 1. Validate thời gian
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc.");
        }

        // 2. Lấy các đối tượng liên quan
        ClassDefinition classDef = classDefinitionRepository.findById(request.getClassDefinitionId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Loại Lớp ID: " + request.getClassDefinitionId()));

        User instructor = userRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy HLV ID: " + request.getInstructorId()));

        if (instructor.getRole() != Role.PT && instructor.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("Người dùng được chọn không phải là HLV (PT) hoặc Nhân viên (Staff).");
        }

        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CLB ID: " + request.getClubId()));

        // 3. Tạo
        ScheduledClass newClass = ScheduledClass.builder()
                .classDefinition(classDef)
                .instructor(instructor)
                .club(club)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxCapacity(request.getMaxCapacity())
                .build();

        ScheduledClass savedClass = scheduledClassRepository.save(newClass);
        return ScheduledClassResponseDTO.fromScheduledClass(savedClass);
    }

    @Transactional
    public void delete(Long scheduledClassId) {
        if (!scheduledClassRepository.existsById(scheduledClassId)) {
            throw new EntityNotFoundException("Không tìm thấy lịch lớp ID: " + scheduledClassId);
        }

        // KIỂM TRA MỚI: Không cho xóa nếu đã có người đặt
        if (classBookingRepository.existsByScheduledClassId(scheduledClassId)) {
            throw new IllegalStateException("Không thể xóa lớp này vì đã có hội viên đặt chỗ.");
        }

        scheduledClassRepository.deleteById(scheduledClassId);
    }
}