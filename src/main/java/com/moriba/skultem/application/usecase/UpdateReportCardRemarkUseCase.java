package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.application.mapper.SchoolMapper;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateReportCardRemarkUseCase {
    private final ReportCardRepository repo;
    private final SchoolRepository schoolRepo;
    private final GetReportCardSettingUseCase getReportCardSettingUseCase;

    public ReportCardDTO execute(String schoolId, String id, String remark) {
        var card = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report card not found"));

        card.updateRemark(remark);
        repo.save(card);

        var school = schoolRepo.findById(schoolId).map(SchoolMapper::toDTO).orElse(null);
        var settings = getReportCardSettingUseCase.execute(schoolId);

        return ReportCardMapper.toDTO(card, school, settings);
    }
}
