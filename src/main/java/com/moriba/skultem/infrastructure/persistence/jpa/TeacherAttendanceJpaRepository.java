package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.infrastructure.persistence.entity.TeacherAttendanceEntity;

public interface TeacherAttendanceJpaRepository extends JpaRepository<TeacherAttendanceEntity, String> {
    Optional<TeacherAttendanceEntity> findByTeacher_IdAndSchoolIdAndDate(String teacherId, String schoolId,
            LocalDate date);

    List<TeacherAttendanceEntity> findAllBySchoolIdAndDate(String schoolId, LocalDate date);

    List<TeacherAttendanceEntity> findAllBySchoolId(String schoolId);

    List<TeacherAttendanceEntity> findAllBySchoolIdAndTeacher_IdAndDateBetween(String schoolId, String teacherId,
            LocalDate from, LocalDate to);
}
