package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListEnrollmentByClassUseCase {
    private final EnrollmentRepository repo;
    private final AcademicYearRepository academicYearRepo;

    public Page<StudentDTO> execute(String schoolId, String classId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));
        return repo.findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), schoolId, pageable).map(e -> {
            return StudentMapper.toDTO(e.getStudent(), e);
        });
    }
}
