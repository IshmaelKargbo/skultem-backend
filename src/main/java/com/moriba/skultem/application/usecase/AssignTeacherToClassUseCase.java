package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Teacher.Status;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignTeacherToClassUseCase {

        private final TeacherRepository teacherRepo;
        private final ClassRepository classRepo;
        private final ClassSessionRepository sessionRepo;
        private final AcademicYearRepository academicYearRepo;
        private final StreamRepository streamRepo;
        private final ClassMasterRepository repo;
        private final ReferenceGeneratorUsecase rg;

        public void execute(String schoolId, String classId, String teacherId, String sectionId, String streamId) {

                AcademicYear academicYear = academicYearRepo.findActiveBySchool(schoolId)
                                .orElseThrow(() -> new NotFoundException("No active academic year found"));

                if (academicYear.isLocked()) {
                        throw new RuleException("Cannot assign class master in a locked academic year");
                }

                Clazz clazz = classRepo.findByIdAndSchool(classId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Teacher not found"));

                if (teacher.getStatus() != Status.ACTIVE) {
                        throw new RuleException("Inactive teacher cannot be assigned as class master");
                }

                ClassSession session;

                if (Level.SSS.equals(clazz.getLevel())) {

                        if (streamId == null) {
                                throw new RuleException("Stream is required for SSS classes");
                        }

                        streamRepo.findByIdAndSchoolId(streamId, schoolId)
                                        .orElseThrow(() -> new NotFoundException("Stream not found"));

                        session = sessionRepo
                                        .findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                                                        classId, academicYear.getId(), sectionId, streamId, schoolId)
                                        .orElseThrow(() -> new NotFoundException("Class session not found"));

                } else {
                        session = sessionRepo
                                        .findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(classId,
                                                        academicYear.getId(), sectionId,
                                                        schoolId)
                                        .orElseThrow(() -> new NotFoundException("Class session not found"));
                }

                var activeMaster = repo
                                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(session.getId());

                if (activeMaster.isPresent()) {
                        throw new RuleException(
                                        "This class session already has an active class master. End the current one first.");
                }

                String id = rg.generate("CLASS_MASTER", "CMR");
                ClassMaster record = ClassMaster.create(id, schoolId, session, teacher);

                repo.save(record);
        }
}
