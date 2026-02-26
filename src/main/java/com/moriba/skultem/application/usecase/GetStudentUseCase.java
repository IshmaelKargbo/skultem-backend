package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.StudentMapper;
import com.moriba.skultem.domain.repository.StudentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetStudentUseCase {
    private final StudentRepository repo;

    public StudentDTO execute(String id, String schoolId) {
        var student = repo.findByIdAndSchoolId(id, schoolId).orElseThrow(() -> new NotFoundException("student not found"));
        return StudentMapper.toDTO(student);
    }
}
