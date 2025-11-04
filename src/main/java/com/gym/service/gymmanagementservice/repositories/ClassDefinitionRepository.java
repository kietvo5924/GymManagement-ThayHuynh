package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.ClassDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, Long> {
}