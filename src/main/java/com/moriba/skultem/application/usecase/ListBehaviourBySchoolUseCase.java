package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.BehaviourRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListBehaviourBySchoolUseCase {
    private final BehaviourRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public Page<BehaviourDTO> execute(String schoolId, String classId, int page, int size) {
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        if (classId != null && !classId.isEmpty())
            return repo.findAllAcademicYearAndSchoolId(academicYear.getId(), schoolId, pageable)
                    .map(BehaviourMapper::toDTO);

        return repo.findAllAcademicYearAndClassIdAndSchoolId(academicYear.getId(), classId, schoolId, pageable)
                .map(BehaviourMapper::toDTO);
    }
}
