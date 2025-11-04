package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set; // <-- IMPORT MỚI

@Entity
@Table(name = "packages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GymPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false)
    private PackageType packageType; // Giữ nguyên: GYM_ACCESS, PT_SESSION, PER_VISIT

    @Column(name = "duration_days")
    private Integer durationDays; // Dùng cho GYM_ACCESS, PER_VISIT

    @Column(name = "number_of_sessions")
    private Integer numberOfSessions; // Dùng cho PT_SESSION, PER_VISIT

    @Column(name = "start_time_limit")
    private LocalTime startTimeLimit; // Giờ check-in sớm nhất (HH:mm)

    @Column(name = "end_time_limit")
    private LocalTime endTimeLimit; // Giờ check-in trễ nhất (HH:mm)

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type") // Mặc định là SINGLE_CLUB nếu không set
    private PackageAccessType accessType; // SINGLE_CLUB hoặc ALL_CLUBS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_club_id")
    private Club targetClub;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "package_amenities",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<Amenity> amenities;


    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (accessType == null) {
            accessType = PackageAccessType.SINGLE_CLUB;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}