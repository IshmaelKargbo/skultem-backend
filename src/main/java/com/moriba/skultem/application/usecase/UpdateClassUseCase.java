package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateClassUseCase {

    private final ClassRepository classRepo;

    @AuditLogAnnotation(action = "CLASS_UPDATED")
    public ClassDTO execute(String schoolId, String classId, String name, Integer levelOrder) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (!clazz.getName().equalsIgnoreCase(name) && classRepo.existsByNameAndSchool(name, schoolId)) {
            throw new AlreadyExistsException("Class with name '" + name + "' already exists in this school.");
        }
        // Not sent by the UI anymore - a missing one leaves the class where it is in lists.
        int order = levelOrder != null ? levelOrder : clazz.getDisplayOrder();
        if (order != clazz.getDisplayOrder() && classRepo.existsByLevelOrderAndSchool(order, schoolId)) {
            throw new AlreadyExistsException(
                    "Class with level order '" + order + "' already exists in this school.");
        }

        clazz.rename(name, order);
        classRepo.save(clazz);

        return ClassMapper.toDTO(clazz);
    }
}
