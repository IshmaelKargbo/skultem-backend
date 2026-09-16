package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.TeacherAttendanceRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Undoes today's most recent clock event for a teacher - for correcting a mistaken clock, same
// today-only scope as AdminClockIn/OutUseCase (this only ever needs to fix something that just
// happened, not rewrite history). Clock-out is undone in place (clock-in stays, status untouched
// - clock-out never set it). Clock-in is undone by deleting the row entirely rather than just
// clearing the clock fields: clocking in is the only thing that created the row and set
// status=PRESENT, so undoing it should put the teacher fully back to "not marked" - not leave a
// stale PRESENT status behind with no clock event to justify it.
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUnclockUseCase {
    private final TeacherAttendanceRepository attendanceRepo;
    private final TeacherRepository teacherRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_ATTENDANCE_UNCLOCKED_BY_ADMIN")
    public void execute(String schoolId, String teacherId) {
        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        var today = LocalDate.now(SchoolTimeZone.ZONE);
        var attendance = attendanceRepo.findByTeacherIdAndSchoolIdAndDate(teacher.getId(), schoolId, today)
                .orElseThrow(() -> new BadRequestException("This teacher hasn't been clocked in today yet"));

        if (attendance.alreadyClockedOut()) {
            attendance.adminUnclockOut();
            attendanceRepo.save(attendance);
        } else if (attendance.alreadyClockedIn()) {
            attendanceRepo.delete(attendance);
        } else {
            throw new BadRequestException("This teacher hasn't been clocked in today yet");
        }

        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher clock corrected by admin",
                teacher.getUser().getName(), null, attendance.getId());
    }
}
