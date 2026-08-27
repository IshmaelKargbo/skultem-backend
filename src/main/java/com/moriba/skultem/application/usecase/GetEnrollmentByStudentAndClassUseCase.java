package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.EnrollmentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.EnrollmentMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetEnrollmentByStudentAndClassUseCase {

    private final EnrollmentRepository repo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public EnrollmentDTO execute(String studentId, String school, String academicYearId) {
        AcademicYear academicYear = resolveAcademicYearUseCase.execute(school, academicYearId);
        boolean explicitYear = academicYearId != null && !academicYearId.isBlank();

        // If the student hasn't been enrolled yet for the newly active academic year (e.g. right after
        // year rollover, before re-enrollment/promotion), fall back to their most recent enrollment so
        // their academic progress/history keeps showing instead of erroring out. Only when nobody
        // asked for a specific year, though - see ListStudentBySchoolUseCase for why.
        var found = repo.findByStudentAndAcademicYearAndSchoolId(studentId, academicYear.getId(), school);
        var res = (explicitYear ? found : found.or(() -> repo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(studentId, school)))
                .orElseThrow(() -> new NotFoundException("enrollment not found"));
        return EnrollmentMapper.toDTO(res);

    }
}
