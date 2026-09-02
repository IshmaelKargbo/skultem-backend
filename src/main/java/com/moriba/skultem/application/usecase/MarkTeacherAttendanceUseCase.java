package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.TeacherAttendance;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.infrastructure.rest.dto.TeacherAttendanceRecordDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Marking the register for a day is a bulk upsert: a teacher already marked for that date gets
// their status/note updated in place, anyone new gets a fresh row - same "set" semantics as
// SetSalaryStructureUseCase, just for a whole day's roster at once.
@Service
@Transactional
@RequiredArgsConstructor
public class MarkTeacherAttendanceUseCase {
    private final TeacherAttendanceRepository repo;
    private final TeacherRepository teacherRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_ATTENDANCE_MARKED")
    public void execute(String schoolId, LocalDate date, List<TeacherAttendanceRecordDTO> records) {
        var toSave = records.stream().map(record -> {
            var existing = repo.findByTeacherIdAndSchoolIdAndDate(record.teacherId(), schoolId, date);

            if (existing.isPresent()) {
                var attendance = existing.get();
                attendance.update(record.status(), record.note());
                return attendance;
            }

            var teacher = teacherRepo.findByIdAndSchoolId(record.teacherId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Teacher not found: " + record.teacherId()));

            return TeacherAttendance.mark(UUID.randomUUID().toString(), schoolId, teacher, date, record.status(),
                    record.note());
        }).toList();

        repo.saveAll(toSave);

        logActivityUseCase.log(
                schoolId,
                ActivityType.TEACHER,
                "Teacher attendance marked",
                date + " (" + toSave.size() + " teachers)",
                null,
                null);
    }
}
