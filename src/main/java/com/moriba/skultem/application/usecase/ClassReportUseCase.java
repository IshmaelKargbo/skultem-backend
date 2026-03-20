package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ClassReportUseCase {

    private final ClassSessionRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassMasterRepository classMasterRepos;
    private final EnrollmentRepository enrollmentRepo;

    public Page<ClassSessionDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();
        var academicYear = academicYearRepo.findActiveBySchool(request.schoolId())
                .orElseThrow(() -> new NotFoundException("no active academic year found"));

        List<Filter> filters = request.filters();

        Page<ClassSession> res = repo.runReport(
                request.schoolId(),
                filters,
                pageable
        );

        return res.map((e) -> {
                    var classMaster = classMasterRepos
                            .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(e.getId());
                    String teacherName = "N/A", teacherId = "";
                    String streamName = "N/A", streamId = "";

                    if (classMaster.isPresent()) {
                        var teacher = classMaster.get().getTeacher();
                        var teacherUser = teacher.getUser();
                        teacherName = teacherUser.getName();
                        teacherId = teacher.getId();
                    }

                    if (e.getStream() != null) {
                        var stream = e.getStream();
                        streamName = stream.getName();
                        streamId = stream.getId();
                    }

                    var section = e.getSection();
                    String sectionName = section.getName(), sectionId = section.getId();

                    var clazz = e.getClazz();
                    String clazzName = clazz.getName(), classId = clazz.getId(), classLevel = clazz.getLevel().name();
                    String grade = "Grade " + clazz.getDisplayOrder();

                    long count = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), request.schoolId(), Pageable.unpaged())
                            .getTotalElements();

                    return new ClassSessionDTO(e.getId(), clazzName, classId, teacherName, teacherId, count, streamName,
                            streamId, sectionName, sectionId, classLevel, grade);
                });
    }
}