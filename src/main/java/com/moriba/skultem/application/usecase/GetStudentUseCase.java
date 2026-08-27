package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentUseCase {
        private final StudentRepository repo;
        private final StudentParentRepository studentParentRepo;
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;
        private final EnrollmentRepository enrollmentRepo;

        public StudentDTO execute(String id, String schoolId, String academicYearId) {
                var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                var student = repo.findByIdAndSchoolId(id, schoolId)
                                .orElseThrow(() -> new NotFoundException("student not found"));

                var studentParent = studentParentRepo.findByStudentAndSchool(student.getId(), schoolId)
                                .orElseThrow(() -> new NotFoundException("student parent not found"));
                
                // Fall back to the student's most recent enrollment when they don't have one yet for the
                // active academic year (e.g. right after year rollover, before re-enrollment/promotion) so
                // their profile and academic history stay visible instead of erroring out.
                var enrollment = enrollmentRepo
                                .findByStudentAndAcademicYearAndSchoolId(student.getId(), academicYear.getId(),
                                                schoolId)
                                .or(() -> enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(
                                                student.getId(), schoolId))
                                .orElse(null);

                return StudentMapper.toDTO(student, enrollment, studentParent.getRelationship());
        }
}
