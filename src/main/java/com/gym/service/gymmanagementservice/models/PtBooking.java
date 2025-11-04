package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pt_bookings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PtBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Gói PT nào đang được sử dụng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_package_id", nullable = false)
    private MemberPackage memberPackage;

    // Hội viên (để tiện truy vấn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // PT nào sẽ dạy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    // Đặt lịch tại CLB nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PtBookingStatus status;

    @Column(name = "notes_by_member", columnDefinition = "TEXT")
    private String notesByMember; // Ghi chú của hội viên khi đặt (vd: "Tập ngực")

    @Column(name = "notes_by_pt", columnDefinition = "TEXT")
    private String notesByPt; // Ghi chú của PT (vd: "Đã hoàn thành tốt")

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}