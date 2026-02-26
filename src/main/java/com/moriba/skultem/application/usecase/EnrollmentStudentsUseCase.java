package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentStudentsUseCase {

        private final StudentRepository studentRepo;
        private final ClassSectionRepository sectionRepo;
        private final ClassStreamRepository streamRepo;
        private final EnrollmentRepository enrollmentRepo;
        private final ClassRepository classRepo;
        private final AcademicYearRepository academicYearRepo;
        private final ReferenceGeneratorUsecase rg;

        public void execute(EnrollData param) {

                var academicYear = academicYearRepo.findActiveBySchool(param.schoolId)
                                .orElseThrow(() -> new NotFoundException("Academic year not found"));

                var clazz = classRepo.findByIdAndSchool(param.classId, param.schoolId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                var section = sectionRepo.findByIdAndClassIdAndSchoolId(param.sectionId, clazz.getId(), param.schoolId)
                                .orElseThrow(() -> new NotFoundException("Section not found"));

                for (String studentId : param.students) {

                        var student = studentRepo.findByIdAndSchoolId(studentId, param.schoolId)
                                        .orElseThrow(() -> new NotFoundException("Student not found"));

                        Stream stream = null;

                        if (clazz.getLevel() == Level.SSS) {

                                if (param.streamId == null || param.streamId.isBlank()) {
                                        throw new RuleException("Stream is required for SSS class");
                                }

                                if (!streamRepo.existsByClassIdAndSchoolIdAndStreamId(clazz.getId(), param.schoolId,
                                                param.streamId)) {
                                        throw new RuleException("Stream does not belong to this class");
                                }

                                var res = streamRepo
                                                .findByClassIdAndSchoolIdAndStreamId(clazz.getId(), param.schoolId,
                                                                param.streamId)
                                                .orElseThrow(() -> new NotFoundException("Stream not found"));
                                stream = res.getStream();

                                if (enrollmentRepo
                                                .existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(
                                                                student.getId(), clazz.getId(),
                                                                section.getSection().getId(),
                                                                academicYear.getId(),
                                                                stream.getId(), param.schoolId)) {
                                        throw new AlreadyExistsException(
                                                        "Enrollment already exists for this student in this stream");
                                }

                        } else {
                                if (param.streamId != null && !param.streamId.isBlank()) {
                                        throw new RuleException("Stream is not allowed for this class");
                                }

                                if (enrollmentRepo
                                                .existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(
                                                                student.getId(), clazz.getId(),
                                                                section.getSection().getId(),
                                                                academicYear.getId(), param.schoolId)) {
                                        throw new AlreadyExistsException("Enrollment already exists for this student");
                                }
                        }

                        var enrollmentId = rg.generate("ENROLLMENT", "ERM");
                        var enrollment = Enrollment.create(enrollmentId, param.schoolId, student, clazz,
                                        section.getSection(),
                                        academicYear, stream);

                        enrollmentRepo.save(enrollment);
                }
        }

        public record EnrollData(
                        String schoolId,
                        String classId,
                        List<String> students,
                        String sectionId,
                        String streamId) {
        }
}
