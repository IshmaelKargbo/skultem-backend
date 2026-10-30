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

@Service
@Transactional
@RequiredArgsConstructor
public class CheckOutLibraryVisitUseCase {
    private final LibraryVisitRepository visitRepo;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_VISIT_CHECKED_OUT")
    public LibraryVisitDTO execute(String schoolId, String visitId, String userId) {
        // Locked, so two check-outs of the same visit at once can't both go through.
        var visit = visitRepo.findByIdAndSchoolForUpdate(visitId, schoolId)
                .orElseThrow(() -> new NotFoundException("Visit not found"));
        var now = support.now();

        LibraryAttendanceSupport.rule(() -> visit.checkOut(userId, support.userName(userId), now));
        visitRepo.save(visit);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library check-out", visit.getStudentName(), null,
                visit.getId());

        return LibraryAttendanceDtoMapper.toDTO(visit, now);
    }
}
