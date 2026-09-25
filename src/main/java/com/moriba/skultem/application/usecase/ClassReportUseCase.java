package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Enrollment;
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
        private final GetFeeDetailUsecase getFeeDetailUsecase;
        private final EnrollmentRepository enrollmentRepo;
        private final SectionScopeService sectionScopeService;

        public Page<ClassSessionDTO> execute(ReportBuilderDTO request, int page, int size) {

                Pageable pageable = createPageable(page, size);
                var academicYear = academicYearRepo.findActiveBySchool(request.schoolId())
                                .orElseThrow(() -> new NotFoundException("no active academic year found"));

                List<Filter> filters = request.filters();

                Page<ClassSession> res = repo.runReport(
                                request.schoolId(),
                                filters,
                                sectionScopeService.levels(),
                                pageable);

                return res.map((e) -> {
                        // A session can have more than one active class master (e.g. co-taught classes) -
                        // list every one of them rather than picking just the most recently assigned.
                        var classMasters = classMasterRepos.findAllActiveBySessionIdAndSchoolId(e.getId(),
                                        request.schoolId());
                        String teacherName = "N/A", teacherId = "";
                        String streamName = "N/A", streamId = "";

                        if (!classMasters.isEmpty()) {
                                teacherName = classMasters.stream()
                                                .map(cm -> cm.getTeacher().getUser().getName())
                                                .collect(Collectors.joining(", "));
                                teacherId = classMasters.get(0).getTeacher().getId();
                        }

                        if (e.getStream() != null) {
                                var stream = e.getStream();
                                streamName = stream.getName();
                                streamId = stream.getId();
                        }

                        var section = e.getSection();
                        String sectionName = section.getName(), sectionId = section.getId();

                        var clazz = e.getClazz();
                        String clazzName = clazz.getName(), classId = clazz.getId(),
                                        classLevel = clazz.getLevel().name();
                        String grade = "Grade " + clazz.getDisplayOrder();

                        List<Enrollment> lists = enrollmentRepo
                                        .findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(),
                                                        request.schoolId(),
                                                        Pageable.unpaged())
                                        .getContent();

                        var feeDetail = getFeeDetail(request.schoolId(), lists);

                        return new ClassSessionDTO(e.getId(), clazzName, classId, teacherName, teacherId, lists.size(),
                                        streamName, streamId, sectionName, sectionId, classLevel, grade, feeDetail, false);
                });
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

        private Pageable createPageable(int page, int size) {

                if (size <= 0) {
                        return Pageable.unpaged();
                }

                int pageNumber = Math.max(page - 1, 0);

                return PageRequest.of(pageNumber, size);
        }
}