package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.repository.TimetableRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

// Takes one subject out of a period/day slot. Other options in the same slot stay.
@Service
@RequiredArgsConstructor
public class DeleteTimetableEntryUseCase {

    private final TimetableRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TIMETABLE_ENTRY_DELETED")
    public Timetable execute(String schoolId, String id) {
        var entry = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("timetable entry not found"));

        repo.delete(entry);

        var session = entry.getPeriod().getSession();
        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Subject removed from timetable",
                (session != null ? session.getName() + ": " : "") + "removed "
                        + entry.getTeacherSubject().getSubject().getName() + " from " + entry.getPeriod().getName()
                        + " on " + entry.getDay().getDay(),
                null,
                entry.getId());

        return entry;
    }
}
