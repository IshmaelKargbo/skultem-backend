package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassFeeDetails;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.MetaMapper;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentFeeByClassUseCase {

    private final GetFeeDetailUsecase feeDetailUsecase;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;

    public ClassFeeDetails execute(String schoolId, String sessionId, String termId,
            int page, int size) {

        Pageable pageable = size > 0
                ? PageRequest.of(page - 1, size)
                : Pageable.unpaged();

        var list = getStudents(schoolId, sessionId, pageable);

        List<ClassFeeDetails.Record> records = new ArrayList<>();

        for (Student student : list) {
            var fee = feeDetailUsecase.execute(
                    schoolId,
                    student.getId(),
                    termId);

            String status = fee.status();

            records.add(
                    new ClassFeeDetails.Record(
                            student.getId(),
                            student.getName(),
                            student.getAdmissionNumber(),
                            status));
        }

        var summery = getSummery(sessionId, schoolId, termId);
        var meta = MetaMapper.toMeta(list);

        return new ClassFeeDetails(sessionId, termId, summery, records, meta);
    }

    private Page<Student> getStudents(String schoolId, String sessionId, Pageable pageable) {
        var year = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));

        return enrollmentRepo.findAllByClassAndAcademicAndSchoolId(
                sessionId,
                year.getId(),
                schoolId,
                pageable).map(e -> e.getStudent());
    }

    private ClassFeeDetails.Summery getSummery(String sessionId, String schoolId, String termId) {

        var students = getStudents(schoolId, sessionId, Pageable.unpaged());

        int paid = 0;
        int pending = 0;
        int partial = 0;

        for (Student student : students) {

            var fee = feeDetailUsecase.execute(
                    schoolId,
                    student.getId(),
                    termId);
                    
            if (fee.status().equals("Paid")) {
                paid++;
            } else if (fee.status().equals("Pending")) {
                pending++;
            } else {
                partial++;
            }
        }

        return new ClassFeeDetails.Summery(paid, pending, partial);
    }
}