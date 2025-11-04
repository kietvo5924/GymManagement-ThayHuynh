package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Amenity;
import com.gym.service.gymmanagementservice.repositories.AmenityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public List<Amenity> getAllAmenities() {
        return amenityRepository.findAll();
    }

    public Amenity getAmenityById(Long id) {
        return amenityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tiện ích với ID: " + id));
    }

    @Transactional
    public Amenity createAmenity(Amenity amenity) {
        return amenityRepository.save(amenity);
    }

    @Transactional
    public Amenity updateAmenity(Long id, Amenity amenityDetails) {
        Amenity existingAmenity = getAmenityById(id);
        existingAmenity.setName(amenityDetails.getName());
        existingAmenity.setDescription(amenityDetails.getDescription());
        return amenityRepository.save(existingAmenity);
    }

    @Transactional
    public void deleteAmenity(Long id) {
        // (Lưu ý: Nếu tiện ích đã được gán cho gói tập, việc xóa có thể gây lỗi
        // Tạm thời cho phép xóa để đơn giản)
        if (!amenityRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy tiện ích với ID: " + id);
        }
        amenityRepository.deleteById(id);
    }
}