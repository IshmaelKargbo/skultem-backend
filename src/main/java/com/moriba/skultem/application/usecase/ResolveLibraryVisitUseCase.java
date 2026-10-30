package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryAttendanceDtoMapper;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Follows up a visit the student left without being checked out (docs/LIBRARY_ATTENDANCE.md §5.3). The visit is
 * closed as "check-out not recorded", with who did it, when and why. No check-out time is made up.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ResolveLibraryVisitUseCase {
    private final LibraryVisitRepository visitRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_VISIT_RESOLVED")
    public LibraryVisitDTO execute(String schoolId, String visitId, String reason, String userId) {
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var now = support.now();

        LibraryAttendanceSupport.rule(() -> visit.resolve(userId, support.userName(userId), reason, now));
        visitRepo.save(visit);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library visit followed up",
                visit.getStudentName() + " - check-out not recorded", null, visit.getId());

        return LibraryAttendanceDtoMapper.toDTO(visit, now);
    }
}
