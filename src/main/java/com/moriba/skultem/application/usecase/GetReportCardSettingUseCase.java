package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.mapper.ReportCardSettingMapper;
import com.moriba.skultem.domain.repository.ReportCardSettingRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReportCardSettingUseCase {
    private final ReportCardSettingRepository repo;
    private final SchoolRepository schoolRepo;

    public ReportCardSettingDTO execute(String schoolId) {
        var existing = repo.findBySchoolId(schoolId);
        if (existing.isPresent()) {
            return ReportCardSettingMapper.toDTO(existing.get());
        }

        var school = schoolRepo.findById(schoolId).orElse(null);
        String headerColor = school != null && school.getPrimaryColor() != null ? school.getPrimaryColor()
                : "#1878c5";
        String logoUrl = school != null && school.getLogo() != null ? school.getLogo() : "";

        return new ReportCardSettingDTO(headerColor, logoUrl, "", true, true, true, true, true);
    }
}
