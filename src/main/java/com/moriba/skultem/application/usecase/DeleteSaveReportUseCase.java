package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.SaveReportRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteSaveReportUseCase {
    private final SaveReportRepository repo;

    public void execute(String schoolId, String id) {
        var record = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Save report not found"));
        repo.delete(record);
    }
}
