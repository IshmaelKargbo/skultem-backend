package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.ReportCardRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TrackReportCardDownloadUseCase {
    private final ReportCardRepository repo;

    public void execute(String schoolId, String id) {
        var card = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Report card not found"));

        card.trackDownload();
        repo.save(card);
    }
}
