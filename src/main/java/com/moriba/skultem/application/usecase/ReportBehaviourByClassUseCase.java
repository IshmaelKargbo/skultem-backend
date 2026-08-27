package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.repository.BehaviourRepository;
import com.moriba.skultem.domain.vo.KindCount;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportBehaviourByClassUseCase {
    private final BehaviourRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public List<KindCount> execute(String schoolId, String classId, String academicYearId, int page, int size) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        return repo.countByKindForClassOrAll(academicYear.getId(), schoolId, classId);
    }
}
