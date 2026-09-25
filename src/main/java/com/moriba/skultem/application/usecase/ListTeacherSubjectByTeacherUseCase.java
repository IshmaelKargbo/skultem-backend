package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

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
    private final SectionScopeService sectionScopeService;

    public List<TeacherSubjectDTO> execute(String school, String teacherId) {
        var scope = sectionScopeService.currentOrAll();
        return repo.findByTeacherIdAndSchoolId(teacherId, school).stream()
                .filter(t -> scope.allows(t.getSession().getClazz().getLevel()))
                .map(TeacherSubjectMapper::toDTO)
                .toList();
    }

    public List<TeacherSubjectDTO> executeByUser(String school, String teacherId) {
        var teacher = repo.findByUser(teacherId, school).orElseThrow(() -> new NotFoundException("Teacher not found"));
        var scope = sectionScopeService.currentOrAll();
        return repo.findByTeacherIdAndSchoolId(teacher.getTeacher().getId(), school).stream()
                // A teacher limited to management sections only sees their subjects inside them.
                .filter(t -> scope.allows(t.getSession().getClazz().getLevel()))
                .map(TeacherSubjectMapper::toDTO)
                .toList();
    }
}
