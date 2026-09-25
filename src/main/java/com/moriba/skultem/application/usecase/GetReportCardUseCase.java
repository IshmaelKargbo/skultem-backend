package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.application.services.SchoolBrandingResolver;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReportCardUseCase {
    private final ReportCardRepository repo;
    private final SchoolRepository schoolRepo;
    private final ClassRepository classRepo;
    private final SchoolBrandingResolver brandingResolver;
    private final GetReportCardSettingUseCase getReportCardSettingUseCase;

    public ReportCardDTO execute(String schoolId, String id) {
        var card = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report card not found"));

        // The card carries its class's management section's own logo / principal / location when it has them.
        var level = classRepo.findByIdAndSchool(card.getClassId(), schoolId).map(Clazz::getLevel).orElse(null);
        var school = schoolRepo.findById(schoolId)
                .map(s -> brandingResolver.schoolDtoFor(s, level))
                .orElse(null);

        ReportCardSettingDTO settings = getReportCardSettingUseCase.execute(schoolId);

        return ReportCardMapper.toDTO(card, school, settings, level);
    }
}
