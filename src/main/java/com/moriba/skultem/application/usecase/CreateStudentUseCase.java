package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.model.Student.Gender;
import com.moriba.skultem.domain.repository.StudentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateStudentUseCase {

    private final StudentRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public StudentDTO execute(CreateStudent param) {

        String admissionNumber = param.admissionNumber().trim().toUpperCase();

        if (repo.existsByAdmissionNumberAndSchoolId(admissionNumber, param.schoolId)) {
            throw new RuleException("admission number already exists");
        }

        String id = rg.generate("STUDENT", "STD");

        Student student = Student.create(
                id,
                param.schoolId,
                admissionNumber,
                param.givenNames().trim(),
                param.familyName().trim(),
                param.gender(),
                param.dateOfBirth());

        repo.save(student);

        return StudentMapper.toDTO(student);
    }

    public record CreateStudent(
            String schoolId,
            String givenNames,
            String familyName,
            String admissionNumber,
            Gender gender,
            LocalDate dateOfBirth) {
    }
}
