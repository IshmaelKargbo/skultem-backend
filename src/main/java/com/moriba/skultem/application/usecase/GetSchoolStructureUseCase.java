package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetSchoolStructureUseCase {

    private final SchoolRepository schoolRepo;
    private final SchoolLevelRepository schoolLevelRepo;
    private final ManagementSectionRepository managementSectionRepo;
    private final ClassRepository classRepo;

    public SchoolStructureDTO execute(String schoolId) {
        var school = schoolRepo.findById(schoolId)
                .orElseThrow(() -> new NotFoundException("School not found"));

        List<SchoolLevel> levels = schoolLevelRepo.findBySchoolId(schoolId);

        var levelDTOs = levels.stream()
                .map(l -> new SchoolStructureDTO.LevelDTO(l.getLevel(), l.getLevel().getLabel(),
                        l.getManagementSectionId(), classRepo.countActiveBySchoolAndLevel(schoolId, l.getLevel())))
                .toList();

        var sectionDTOs = managementSectionRepo.findBySchoolId(schoolId).stream()
                .map(s -> new SchoolStructureDTO.ManagementSectionDTO(s.getId(), s.getName(), s.getDisplayOrder(),
                        levels.stream()
                                .filter(l -> s.getId().equals(l.getManagementSectionId()))
                                .map(SchoolLevel::getLevel)
                                .toList(),
                        s.getLogo(), s.getPrincipalName(), s.getPrincipalSignature(), s.getAddress()))
                .toList();

        return new SchoolStructureDTO(school.getManagementModel(), levelDTOs, sectionDTOs);
    }
}
