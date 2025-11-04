package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "class_bookings")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClassBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hội viên nào đặt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // Đặt cho lớp nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_class_id", nullable = false)
    private ScheduledClass scheduledClass;

    @Column(name = "booking_time", nullable = false)
    private OffsetDateTime bookingTime; // Thời điểm hội viên bấm nút "Book"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @PrePersist
    protected void onCreate() {
        bookingTime = OffsetDateTime.now();
        status = BookingStatus.BOOKED;
    }
}