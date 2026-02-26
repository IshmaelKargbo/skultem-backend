package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetClassSessionUseCase {

    private final ClassSessionRepository repo;
    private final ClassMasterRepository classMasterRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final AcademicYearRepository academicYearRepo;

    public ClassSessionDTO execute(String id, String school) {
        var academicYear = academicYearRepo.findByIdAndSchoolId(id, school)
                .orElseThrow(() -> new NotFoundException("Active academic year not found"));

        var domain = repo.findByIdAndSchoolId(id, school)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var classMaster = classMasterRepo
                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(domain.getId());
        String teacherName = "N/A", teacherId = "";
        String streamName = "N/A", streamId = "";

        if (classMaster.isPresent()) {
            var teacher = classMaster.get().getTeacher();
            var teacherUser = teacher.getUser();
            teacherName = teacherUser.getName();
            teacherId = teacher.getId();
        }

        if (domain.getStream() != null) {
            var stream = domain.getStream();
            streamName = stream.getName();
            streamId = stream.getId();
        }

        var section = domain.getSection();
        String sectionName = section.getName(), sectionId = section.getId();

        var clazz = domain.getClazz();
        String clazzName = clazz.getName(), classId = clazz.getId(), classLevel = clazz.getLevel().name();

        long count = enrollmentRepo.findAllByClassAndAcademicSchoolId(classId, academicYear.getId(), school)
                .size();

        return new ClassSessionDTO(domain.getId(), clazzName, classId, teacherName, teacherId, count, streamName,
                streamId, sectionName, sectionId, classLevel);
    }
}
