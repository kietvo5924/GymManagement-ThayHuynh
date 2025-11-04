package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}