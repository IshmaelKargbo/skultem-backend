package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.domain.model.Attendance;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.AttendanceJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AttendanceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttendanceAdapter implements AttendanceRepository {

    private final AttendanceJpaRepository repo;

    @Override
    public void save(Attendance domain) {
        var entity = AttendanceMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(Attendance domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public Optional<Attendance> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(AttendanceMapper::toDomain);
    }

    @Override
    public Optional<Attendance> findByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date,
            String schoolId) {
        return repo.findByEnrollment_IdAndDateAndSchoolId(enrollmentId, date, schoolId).map(AttendanceMapper::toDomain);
    }

    @Override
    public boolean existsByEnrollmentAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId) {
        return repo.existsByEnrollment_IdAndDateAndSchoolId(enrollmentId, date, schoolId);
    }

    @Override
    public Page<Attendance> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(AttendanceMapper::toDomain);
    }

    @Override
    public Page<Attendance> findByEnrollmentAndSchoolId(String enrollmentId, String schoolId, Pageable pageable) {
        return repo.findAllByEnrollment_IdAndSchoolId(enrollmentId, schoolId, pageable).map(AttendanceMapper::toDomain);
    }

    @Override
    public Page<AttendanceHistoryDTO> fetchDailyClassAttendanceSummary(String classId, String academicYear,
            String schoolId, Pageable pageable) {
                return repo.fetchDailyClassAttendanceSummary(schoolId, classId, academicYear, null, null, pageable);
    }

    @Override
    public List<Object[]> weeklyAttendance(String schoolId, LocalDate start, LocalDate end) {
        return repo.weeklyAttendance(schoolId, start, end);
    }
}
