package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.vo.GradeApprover;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Sets (or clears) who approves grades for one management section. Null means the school's choice.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateSectionGradeApproverUseCase {

    private final SchoolRepository schoolRepo;
    private final ManagementSectionRepository sectionRepo;
    private final GetSchoolStructureUseCase getSchoolStructureUseCase;

    public SchoolStructureDTO execute(String schoolId, String sectionId, GradeApprover gradeApprover) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("School not found"));
        if (school.getManagementModel() != ManagementModel.SECTION_BASED) {
            throw new RuleException("This school isn't managed in sections");
        }
        var section = sectionRepo.findBySchoolId(schoolId).stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Management section not found"));

        section.updateGradeApprover(gradeApprover);
        sectionRepo.save(section);

        return getSchoolStructureUseCase.execute(schoolId);
    }
}
