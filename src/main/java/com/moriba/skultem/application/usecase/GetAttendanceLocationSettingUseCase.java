package com.moriba.skultem.application.usecase;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceLocationSettingDTO;
import com.moriba.skultem.application.mapper.AttendanceLocationSettingMapper;
import com.moriba.skultem.domain.repository.AttendanceLocationSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAttendanceLocationSettingUseCase {
    private final AttendanceLocationSettingRepository repo;

    // The school-wide location.
    public AttendanceLocationSettingDTO execute(String schoolId) {
        return AttendanceLocationSettingMapper.toDTO(repo.findBySchoolId(schoolId).orElse(null));
    }

    // The locations configured for management sections. sectionIds limits it to those sections
    // (a section-limited caller's own); null means every section's.
    public List<AttendanceLocationSettingDTO> executeForSections(String schoolId, Collection<String> sectionIds) {
        return repo.findAllBySchoolId(schoolId).stream()
                .filter(l -> l.getManagementSectionId() != null)
                .filter(l -> sectionIds == null || sectionIds.contains(l.getManagementSectionId()))
                .map(l -> AttendanceLocationSettingMapper.toDTO(l))
                .toList();
    }
}
