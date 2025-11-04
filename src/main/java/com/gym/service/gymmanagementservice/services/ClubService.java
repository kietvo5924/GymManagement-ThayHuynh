package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Club;
import com.gym.service.gymmanagementservice.repositories.ClubRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;

    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    public List<Club> getAllActiveClubs() {
        return clubRepository.findAllByIsActive(true);
    }

    public Club getClubById(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CLB với ID: " + id));
    }

    @Transactional
    public Club createClub(Club club) {
        return clubRepository.save(club);
    }

    @Transactional
    public Club updateClub(Long id, Club clubDetails) {
        Club existingClub = getClubById(id);
        existingClub.setName(clubDetails.getName());
        existingClub.setAddress(clubDetails.getAddress());
        existingClub.setPhoneNumber(clubDetails.getPhoneNumber());
        return clubRepository.save(existingClub);
    }

    @Transactional
    public void toggleClubStatus(Long id) {
        Club existingClub = getClubById(id);
        existingClub.setActive(!existingClub.isActive());
        clubRepository.save(existingClub);
    }
}