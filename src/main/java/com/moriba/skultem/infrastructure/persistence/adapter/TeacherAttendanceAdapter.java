package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TeacherAttendanceJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TeacherAttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeacherAttendanceAdapter implements TeacherAttendanceRepository {
    private final TeacherAttendanceJpaRepository repo;

    @Override
    public void save(TeacherAttendance domain) {
        repo.save(TeacherAttendanceMapper.toEntity(domain));
    }

    @Override
    public void saveAll(List<TeacherAttendance> domains) {
        repo.saveAll(domains.stream().map(TeacherAttendanceMapper::toEntity).toList());
    }

    @Override
    public Optional<TeacherAttendance> findByTeacherIdAndSchoolIdAndDate(String teacherId, String schoolId,
            LocalDate date) {
        return repo.findByTeacher_IdAndSchoolIdAndDate(teacherId, schoolId, date).map(TeacherAttendanceMapper::toDomain);
    }

    @Override
    public List<TeacherAttendance> findAllBySchoolIdAndDate(String schoolId, LocalDate date) {
        return repo.findAllBySchoolIdAndDate(schoolId, date).stream().map(TeacherAttendanceMapper::toDomain).toList();
    }

    @Override
    public List<TeacherAttendance> findAllBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(TeacherAttendanceMapper::toDomain).toList();
    }

    @Override
    public List<TeacherAttendance> findAllBySchoolIdAndTeacherIdBetween(String schoolId, String teacherId,
            LocalDate from, LocalDate to) {
        return repo.findAllBySchoolIdAndTeacher_IdAndDateBetween(schoolId, teacherId, from, to).stream()
                .map(TeacherAttendanceMapper::toDomain)
                .toList();
    }
}
