package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibrarySettingDTO;
import com.moriba.skultem.application.services.LibraryBorrowingRules;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.LibrarySetting;
import com.moriba.skultem.domain.repository.LibrarySettingRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateLibrarySettingUseCase {
    private final LibrarySettingRepository repo;
    private final LogActivityUseCase logActivityUseCase;
    private final LibraryBorrowingRules rules;

    // sectionId null saves the school's rules; otherwise that section's own (created on first save).
    @AuditLogAnnotation(action = "LIBRARY_SETTINGS_UPDATED")
    public LibrarySettingDTO execute(String schoolId, String sectionId, int maxStudentLoans, int maxTeacherLoans,
            boolean blockWhenOverdue, int defaultLoanDays) {
        LibrarySetting setting = sectionId == null
                ? repo.findBySchool(schoolId).orElseGet(() -> LibrarySetting.defaults(schoolId))
                : repo.findBySchoolAndSection(schoolId, sectionId)
                        .orElseGet(() -> LibrarySetting.forSection(sectionId, rules.settingFor(schoolId, null)));
        setting.update(maxStudentLoans, maxTeacherLoans, blockWhenOverdue, defaultLoanDays);
        repo.save(setting);

        logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library settings updated",
                "Students " + maxStudentLoans + ", teachers " + maxTeacherLoans, null, setting.getId());

        return toDTO(setting, sectionId, true);
    }

    public static LibrarySettingDTO toDTO(LibrarySetting s, String sectionId, boolean ownRules) {
        return new LibrarySettingDTO(s.getMaxStudentLoans(), s.getMaxTeacherLoans(), s.isBlockWhenOverdue(),
                s.getDefaultLoanDays(), sectionId, ownRules);
    }
}
