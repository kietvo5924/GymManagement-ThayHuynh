package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "scheduled_classes")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduledClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lớp này là lớp gì? (Vd: Yoga)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_definition_id", nullable = false)
    private ClassDefinition classDefinition;

    // Ai dạy lớp này? (Phải là PT/Staff)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor; // Liên kết tới User (Role PT)

    // Lớp này diễn ra ở đâu?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity; // Số lượng hội viên tối đa
}