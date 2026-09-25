package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.StaffScopeDTO;
import com.moriba.skultem.domain.model.StaffManagementSection;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;

import lombok.RequiredArgsConstructor;

// Every section-limited (user, role) in a school - anyone not listed is whole-school.
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListStaffManagementSectionsUseCase {

    private final StaffManagementSectionRepository staffSectionRepo;

    public List<StaffScopeDTO> execute(String schoolId) {
        Map<String, StaffScopeDTO> byKey = new LinkedHashMap<>();
        for (StaffManagementSection row : staffSectionRepo.findBySchoolId(schoolId)) {
            String key = row.getUserId() + "|" + row.getRole();
            var existing = byKey.get(key);
            List<String> ids = existing == null ? new ArrayList<>() : new ArrayList<>(existing.sectionIds());
            ids.add(row.getManagementSectionId());
            byKey.put(key, new StaffScopeDTO(row.getUserId(), row.getRole(), ids));
        }
        return List.copyOf(byKey.values());
    }
}
