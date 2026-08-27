package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardStatsDTO;
import com.moriba.skultem.domain.repository.ReportCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetReportCardStatsUseCase {
    private final ReportCardRepository repo;

    public ReportCardStatsDTO execute(String schoolId) {
        long total = repo.countBySchoolId(schoolId);
        long passed = repo.countBySchoolIdAndPassed(schoolId, true);
        long failed = total - passed;
        long downloads = repo.sumDownloadsBySchoolId(schoolId);

        return new ReportCardStatsDTO(total, passed, failed, downloads);
    }
}
