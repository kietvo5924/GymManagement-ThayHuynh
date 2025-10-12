package com.gym.service.gymmanagementservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface PackageRepository extends JpaRepository<com.gym.service.gymmanagementservice.models.Package, Long> {
    Optional<com.gym.service.gymmanagementservice.models.Package> findByName(String name);
}
