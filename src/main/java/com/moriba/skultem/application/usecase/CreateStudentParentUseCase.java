package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentParentRequest;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.StudentParent;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateStudentParentUseCase {

    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final StudentParentRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "STUDENT_PARENT_CREATED")
    public void execute(StudentParentRequest param) {
        var parent  = parentRepo.findById(param.parent()).orElseThrow(() -> new NotFoundException("parent not found"));
        var student  = studentRepo.findByIdAndSchoolId(param.student(), param.school()).orElseThrow(() -> new NotFoundException("student not found"));
        
        var domain = StudentParent.create(param.school(), param.relationship(), parent, student);
        repo.save(domain);

        logActivityUseCase.log(
                param.school(),
                ActivityType.PARENT,
                "New student parent added",
                parent.getUser().getGivenNames() + " " + parent.getUser().getFamilyName(),
                null,
                domain.getId());
    }

}
