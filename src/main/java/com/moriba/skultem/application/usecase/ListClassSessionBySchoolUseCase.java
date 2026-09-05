package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSessionBySchoolUseCase {

    private final ClassSessionRepository repo;
    private final AcademicYearRepository academicYearRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final ClassMasterRepository classMasterRepos;
    private final EnrollmentRepository enrollmentRepo;

    public Page<ClassSessionDTO> execute(String school, int page, int size) {
        return execute(school, null, page, size);
    }

    public Page<ClassSessionDTO> execute(String school, String academicYearId, int page, int size) {
        return execute(school, academicYearId, page, size, null, null, null);
    }

    public Page<ClassSessionDTO> execute(String school, String academicYearId, int page, int size, String sectionId,
            String streamId, String query) {
        var academicYear = (academicYearId != null && !academicYearId.isBlank())
                ? academicYearRepo.findByIdAndSchoolId(academicYearId, school)
                        .orElseThrow(() -> new NotFoundException("Academic year not found"))
                : academicYearRepo.findActiveBySchool(school)
                        .orElseThrow(() -> new NotFoundException("no active academic year found"));
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        boolean hasFilters = (sectionId != null && !sectionId.isBlank()) || (streamId != null && !streamId.isBlank())
                || (query != null && !query.isBlank());
        var sessions = hasFilters
                ? repo.search(school, academicYear.getId(), normalize(sectionId), normalize(streamId),
                        normalize(query), pageable)
                : repo.findBySchoolIdAndAcademicYearId(school, academicYear.getId(), pageable);

        return sessions.map((e) -> toDto(school, academicYear.getId(), e));
    }

    private ClassSessionDTO toDto(String school, String academicYearId, ClassSession e) {
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

        // streamId defaults to "" above (not null) for a session with no stream, so it must
        // be checked with isBlank() here, not != null - a blank streamId was always taking
        // the "has a stream" branch below, looking up stream.id = '' (matching nothing) and
        // making every non-streamed class in this list report 0 students.
        List<Enrollment> lists;
        if (!streamId.isBlank()) {
            lists = enrollmentRepo.findActiveByClassIdAndSectionIdAndStreamIdAndAcademicYearIdAndSchoolId(
                    clazz.getId(), sectionId, streamId, academicYearId, school);
        } else {
            lists = enrollmentRepo.findAllByClassAndAcademicAndSchoolId(classId, academicYearId,
                    school, Pageable.unpaged())
                    .stream()
                    .filter(e1 -> e1.getStream() == null && e1.getSection().getId().equals(sectionId))
                    .toList();
        }

        var feeDetail = getFeeDetail(school, lists);

        return new ClassSessionDTO(e.getId(), clazzName, classId, teacherName, teacherId, lists.size(),
                streamName, streamId, sectionName, sectionId, classLevel, grade, feeDetail);
    }

    // Empty string, never null - see the repository's search query for why.
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }

    private FeeDetail getFeeDetail(String schoolId, List<Enrollment> enrollments) {
        BigDecimal totalExpected = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        String status = "";

        for (Enrollment enrollment : enrollments) {

            FeeDetail detail = getFeeDetailUsecase.execute(schoolId, enrollment.getStudent().getId());

            if (detail != null) {
                if (detail.total() != null)
                    totalExpected = totalExpected.add(detail.total());

                if (detail.paid() != null)
                    totalCollected = totalCollected.add(detail.paid());

                if (detail.balance() != null)
                    totalOutstanding = totalOutstanding.add(detail.balance());

                status = detail.status();
            }
        }

        return new FeeDetail(totalExpected, totalCollected, totalOutstanding, status);
    }
}
