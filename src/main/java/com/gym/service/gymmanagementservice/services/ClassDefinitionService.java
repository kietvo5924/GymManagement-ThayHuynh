package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.ClassDefinition;
import com.gym.service.gymmanagementservice.repositories.ClassDefinitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassDefinitionService {

    private final ClassDefinitionRepository classDefinitionRepository;

    public List<ClassDefinition> getAll() {
        return classDefinitionRepository.findAll();
    }

    public List<ClassDefinition> getAllActive() {
        return classDefinitionRepository.findAll().stream()
                .filter(ClassDefinition::isActive)
                .toList();
    }

    public ClassDefinition getById(Long id) {
        return classDefinitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Loại Lớp với ID: " + id));
    }

    @Transactional
    public ClassDefinition create(ClassDefinition classDef) {
        return classDefinitionRepository.save(classDef);
    }

    @Transactional
    public ClassDefinition update(Long id, ClassDefinition classDefDetails) {
        ClassDefinition existing = getById(id);
        existing.setName(classDefDetails.getName());
        existing.setDescription(classDefDetails.getDescription());
        return classDefinitionRepository.save(existing);
    }

    @Transactional
    public void toggleStatus(Long id) {
        ClassDefinition existing = getById(id);
        existing.setActive(!existing.isActive());
        classDefinitionRepository.save(existing);
    }
}