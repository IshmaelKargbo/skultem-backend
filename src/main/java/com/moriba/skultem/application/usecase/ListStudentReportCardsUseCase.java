package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.domain.repository.ReportCardRepository;

import lombok.RequiredArgsConstructor;

// Every report card a student has ever had generated, most recent first - powers the Report Card
// tab on the student profile page.
@Service
@RequiredArgsConstructor
public class ListStudentReportCardsUseCase {
    private final ReportCardRepository repo;

    public List<ReportCardSummaryDTO> execute(String schoolId, String studentId) {
        return execute(schoolId, studentId, false);
    }

    // approvedOnly: a parent's view - only cards the class teacher has approved.
    public List<ReportCardSummaryDTO> execute(String schoolId, String studentId, boolean approvedOnly) {
        return repo.findAllBySchoolIdAndStudentId(schoolId, studentId).stream()
                .filter(c -> !approvedOnly || c.isApproved())
                .map(ReportCardMapper::toSummaryDTO)
                .toList();
    }
}
