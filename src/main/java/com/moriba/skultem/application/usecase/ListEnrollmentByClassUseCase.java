package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListEnrollmentByClassUseCase {
    private final EnrollmentRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public Page<StudentDTO> execute(String schoolId, String classId, String streamId, String academicYearId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

        if (streamId != null && !streamId.isBlank()) {
            return repo.findAllByClassIdAndStreamIdAndAcademicYearId(classId, streamId, academicYear.getId(), pageable)
                    .map(e -> StudentMapper.toDTO(e.getStudent(), e));
        }

        return repo.findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId, pageable).map(e -> {
            return StudentMapper.toDTO(e.getStudent(), e);
        });
    }
}
