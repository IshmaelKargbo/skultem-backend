package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSessionDTO;
import com.moriba.skultem.application.dto.FeeDetail;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSessionByTeacherUseCase {

    private final AcademicYearRepository academicYearRepo;
    private final TeacherRepository teacherRepo;
    private final GetFeeDetailUsecase getFeeDetailUsecase;
    private final ClassMasterRepository classMasterRepos;
    private final EnrollmentRepository enrollmentRepo;

    public List<ClassSessionDTO> execute(String school, String userId, int page, int size) {
        var academicYear = academicYearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new NotFoundException("no active academic year found"));
        Pageable pageable = Pageable.unpaged();

        var teacher = teacherRepo.findByUserId(userId).orElseThrow(() -> new NotFoundException("Teacher not found"));

        return classMasterRepos.findByTeacherAndAcademicYear(teacher.getId(), academicYear.getId(), pageable)
                .getContent()
                .stream()
                .map((e) -> {
                    String teacherName = "N/A", teacherId = "";
                    String streamName = "N/A", streamId = "";

                    var teacherUser = teacher.getUser();
                    teacherName = teacherUser.getName();
                    teacherId = teacher.getId();

                    var session = e.getSession();

                    if (session.getStream() != null) {
                        var stream = session.getStream();
                        streamName = stream.getName();
                        streamId = stream.getId();
                    }

                    var section = session.getSection();
                    String sectionName = section.getName(), sectionId = section.getId();

                    var clazz = session.getClazz();
                    String clazzName = clazz.getName(), classId = clazz.getId(), classLevel = clazz.getLevel().name();
                    String grade = "Grade " + clazz.getDisplayOrder();

                    List<Enrollment> lists = enrollmentRepo
                            .findAllByClassAndAcademicAndSchoolId(classId, academicYear.getId(), school,
                                    Pageable.unpaged())
                            .getContent();

                    var feeDetail = getFeeDetail(school, lists);

                    return new ClassSessionDTO(session.getId(), clazzName, classId, teacherName, teacherId,
                            lists.size(),
                            streamName, streamId, sectionName, sectionId, classLevel, grade, feeDetail);
                })
                .toList();
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
