package com.moriba.skultem.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import com.moriba.skultem.domain.model.Timetable;

@Entity
@Table(name = "timetables")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private PeriodEntity period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Timetable.Kind kind;

    // Null for a Library period.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_subject_id")
    private TeacherSubjectEntity teacherSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "working_day_id", nullable = false)
    private WorkingDayEntity day;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    private String color;

    private Instant createdAt;
    private Instant updatedAt;
}
