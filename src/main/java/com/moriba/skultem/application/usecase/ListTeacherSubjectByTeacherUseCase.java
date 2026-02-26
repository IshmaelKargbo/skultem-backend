package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTeacherSubjectByTeacherUseCase {

    private final TeacherSubjectRepository repo;

    public List<TeacherSubjectDTO> execute(String school, String teacher) {
        return repo.findByTeacherIdAndSchoolId(teacher, school).stream().map(TeacherSubjectMapper::toDTO).toList();
    }
}
