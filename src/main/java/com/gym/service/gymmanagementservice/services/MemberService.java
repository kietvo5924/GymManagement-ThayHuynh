package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.MemberRequestDTO;
import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberResponseDTO createMember(MemberRequestDTO request) {
        if (memberRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký.");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được đăng ký.");
        }

        String uniqueBarcode = UUID.randomUUID().toString();

        Member member = Member.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .barcode(uniqueBarcode)
                .build();

        return MemberResponseDTO.fromMember(memberRepository.save(member));
    }

    public List<MemberResponseDTO> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponseDTO::fromMember)
                .collect(Collectors.toList());
    }

    // Lấy thông tin một hội viên bằng ID
    public MemberResponseDTO getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));
        return MemberResponseDTO.fromMember(member);
    }

    // Cập nhật thông tin hội viên
    @Transactional
    public MemberResponseDTO updateMember(Long memberId, MemberRequestDTO request) {
        Member existingMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));

        memberRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(member -> {
            if (!Objects.equals(member.getId(), memberId)) {
                throw new IllegalArgumentException("Số điện thoại đã được đăng ký bởi người khác.");
            }
        });
        if(request.getEmail() != null && !request.getEmail().isEmpty()) {
            memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
                if (!Objects.equals(member.getId(), memberId)) {
                    throw new IllegalArgumentException("Email đã được đăng ký bởi người khác.");
                }
            });
        }

        existingMember.setFullName(request.getFullName());
        existingMember.setPhoneNumber(request.getPhoneNumber());
        existingMember.setEmail(request.getEmail());
        existingMember.setBirthDate(request.getBirthDate());
        existingMember.setAddress(request.getAddress());

        Member updatedMember = memberRepository.save(existingMember);
        return MemberResponseDTO.fromMember(updatedMember);
    }
}
