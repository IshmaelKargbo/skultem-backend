package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.vo.Level;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentCreationService {
    private final EnrollmentRepository enrollmentRepo;
    private final ReferenceGeneratorUsecase referenceGenerator;

    public Enrollment create(
            String schoolId,
            Student student,
            Clazz clazz,
            Section section,
            AcademicYear academicYear,
            Stream stream) {
        if (clazz.getLevel() == Level.SSS) {
            if (stream == null) {
                throw new RuleException("Stream is required for SSS class");
            }

            if (enrollmentRepo.existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(
                    student.getId(), clazz.getId(), section.getId(), academicYear.getId(), stream.getId(), schoolId)) {
                throw new AlreadyExistsException("Enrollment already exists for this student in this stream");
            }
        } else {
            if (stream != null) {
                throw new RuleException("Stream is not allowed for this class");
            }

            if (enrollmentRepo.existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(
                    student.getId(), clazz.getId(), section.getId(), academicYear.getId(), schoolId)) {
                throw new AlreadyExistsException("Enrollment already exists for this student");
            }
        }

        var enrollmentId = referenceGenerator.generate("ENROLLMENT", "ERM");
        var enrollment = Enrollment.create(enrollmentId, schoolId, student, clazz, section, academicYear, stream);
        enrollmentRepo.save(enrollment);
        return enrollment;
    }
}
