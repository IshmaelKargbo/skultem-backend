package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.ReportCardSettingDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReportCardUseCase {
    private final ReportCardRepository repo;
    private final SchoolRepository schoolRepo;
    private final GetReportCardSettingUseCase getReportCardSettingUseCase;

    public ReportCardDTO execute(String schoolId, String id) {
        var card = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report card not found"));

        var school = schoolRepo.findById(schoolId)
                .map(SchoolMapper::toDTO)
                .orElse(null);

        ReportCardSettingDTO settings = getReportCardSettingUseCase.execute(schoolId);

        return ReportCardMapper.toDTO(card, school, settings);
    }
}
