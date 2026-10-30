package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibrarySettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Picks the subject whose timetable periods are the school's scheduled library sessions (D-7). Sessions already
 * opened keep their own copy of the subject, so changing it only affects sessions nobody has opened yet.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SetLibrarySubjectUseCase {
    private final LibrarySettingRepository settingRepo;
    private final SubjectRepository subjectRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LIBRARY_SUBJECT_SET")
    public LibrarySettingDTO execute(String schoolId, String subjectId) {
        String subjectName = null;
        if (subjectId != null && !subjectId.isBlank()) {
            subjectName = subjectRepo.findByIdAndSchoolId(subjectId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Subject not found")).getName();
        }

        var setting = settingRepo.findBySchool(schoolId).orElseGet(() -> LibrarySetting.defaults(schoolId));
        setting.changeLibrarySubject(subjectId);
        settingRepo.save(setting);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library subject set",
                subjectName == null ? "No library subject" : subjectName, null, setting.getId());

        return UpdateLibrarySettingUseCase.toDTO(setting);
    }
}
