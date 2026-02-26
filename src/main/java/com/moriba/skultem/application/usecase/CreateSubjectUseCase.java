package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateSubjectUseCase {
    private final SubjectRepository repo;
    private final ReferenceGeneratorUsecase rg;

    public SubjectDTO execute(String schoolId, String name, String code, String description) {
        if (repo.existsByCodeAndSchool(code, schoolId)) {
            throw new AlreadyExistsException("subject code already exist");
        }
        var id = rg.generate("SUBJECT", "SUB");
        var record = Subject.create(id, schoolId, name, code, description);
        repo.save(record);
        return SubjectMapper.toDTO(record);
    }
}
