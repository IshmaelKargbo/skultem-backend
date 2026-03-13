package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.vo.KindCount;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportBehaviourByClassUseCase {
    private final BehaviourRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public List<KindCount> execute(String schoolId, String classId, int page, int size) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        return repo.countByKindForClassOrAll(academicYear.getId(), schoolId, classId);
    }
}
