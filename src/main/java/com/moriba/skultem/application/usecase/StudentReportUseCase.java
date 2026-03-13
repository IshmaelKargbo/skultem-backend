package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FilterDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.vo.Gender;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentReportUseCase {

    private final EnrollmentRepository repo;

    public Page<StudentDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        String classId = null;
        String sectionId = null;
        String streamId = null;
        Enrollment.Status status = null;
        String gender = null;
        String studentName = null;
        String admissionNumber = null;
        LocalDate dobFrom = null;
        LocalDate dobTo = null;

        List<FilterDTO> filters = request.filters();
        for (FilterDTO f : filters) {
            switch (f.field()) {
                case "class" -> classId = f.value();
                case "section" -> sectionId = f.value();
                case "stream" -> streamId = f.value();
                case "status" -> status = Enrollment.Status.valueOf(f.value());
                case "gender" -> gender = f.value();
                case "studentName" -> studentName = f.value();
                case "admissionNumber" -> admissionNumber = f.value();
                case "age" -> {
                    LocalDate today = LocalDate.now();
                    if (f.from() != null) {
                        dobTo = today.minusYears(Integer.parseInt(f.from()));
                    }
                    if (f.to() != null) {
                        dobFrom = today.minusYears(Integer.parseInt(f.to()));
                    }
                }
            }
        }

        return repo.runStudentReport(
                request.schoolId(),
                null, // academicYearId can be handled separately if needed
                classId,
                sectionId,
                streamId,
                status,
                gender != null ? Gender.valueOf(gender) : null,
                studentName,
                admissionNumber,
                dobFrom,
                dobTo,
                pageable).map(e -> StudentMapper.toDTO(e.getStudent(), e));
    }
}