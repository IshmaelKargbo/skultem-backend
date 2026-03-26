package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTeacherSubjectByTeacherUseCase {

    private final TeacherSubjectRepository repo;

    public List<TeacherSubjectDTO> execute(String school, String teacherId) {
        return repo.findByTeacherIdAndSchoolId(teacherId, school).stream().map(TeacherSubjectMapper::toDTO)
                .toList();
    }

    public List<TeacherSubjectDTO> executeByUser(String school, String teacherId) {
        var teacher = repo.findByUser(teacherId, school).orElseThrow(() -> new NotFoundException("Teacher not found"));
        return repo.findByTeacherIdAndSchoolId(teacher.getTeacher().getId(), school).stream().map(TeacherSubjectMapper::toDTO)
                .toList();
    }
}
