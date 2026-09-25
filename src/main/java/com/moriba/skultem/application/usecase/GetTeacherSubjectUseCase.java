package com.moriba.skultem.application.usecase;

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
public class GetTeacherSubjectUseCase {

    private final TeacherSubjectRepository repo;
    private final com.moriba.skultem.application.services.SectionScopeService sectionScopeService;

    public TeacherSubjectDTO execute(String schoolId, String teacherId) {
        var res = repo.findOneByTeacherIdAndSchoolId(teacherId, schoolId)
                .filter(t -> sectionScopeService.currentOrAll().allows(t.getSession().getClazz().getLevel()))
                .orElseThrow(() -> new NotFoundException("teacher subject not found"));
        return TeacherSubjectMapper.toDTO(res);
    }
}
