package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListBehaviourBySchoolUseCase {
    private final BehaviourRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<BehaviourDTO> execute(String schoolId, String classId, String academicYearId, int page, int size) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (classId == null || classId.isEmpty()) {
            return repo.findAllAcademicYearAndSchoolId(academicYear.getId(), schoolId, pageable)
                    .map(BehaviourMapper::toDTO);
        }

        return repo.findAllAcademicYearAndClassIdAndSchoolId(academicYear.getId(), classId, schoolId, pageable)
                .map(BehaviourMapper::toDTO);
    }
}
