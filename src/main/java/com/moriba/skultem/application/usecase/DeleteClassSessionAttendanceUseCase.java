package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteClassSessionAttendanceUseCase {
    private final AttendanceRepository attendanceRepo;
    private final ClassSessionRepository classSessionRepo;
    private final EnrollmentRepository enrollmentRepo;

    public long execute(String schoolId, String classSessionId, LocalDate date) {
        var classSession = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId)
                .orElseThrow(() -> new com.moriba.skultem.application.error.NotFoundException("Class session not found"));

        var enrollments = enrollmentRepo.findAllByClassAndAcademicSchoolId(
                classSession.getClazz().getId(),
                classSession.getAcademicYear().getId(),
                schoolId, Pageable.unpaged()).getContent();

        long deleted = 0;
        for (var enrollment : enrollments) {
            if (enrollment.getSection() == null || classSession.getSection() == null) {
                continue;
            }

            boolean sectionMatch = enrollment.getSection().getId().equals(classSession.getSection().getId());
            if (!sectionMatch) {
                continue;
            }

            boolean streamMatch = classSession.getStream() == null
                    ? enrollment.getStream() == null
                    : enrollment.getStream() != null
                            && enrollment.getStream().getId().equals(classSession.getStream().getId());

            if (!streamMatch) {
                continue;
            }

            var attendance = attendanceRepo.findByEnrollmentAndDateAndSchoolId(enrollment.getId(), date, schoolId);
            if (attendance.isPresent()) {
                attendanceRepo.delete(attendance.get());
                deleted++;
            }
        }

        return deleted;
    }
}
