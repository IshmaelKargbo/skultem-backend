package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMasterMapper;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Unlike GetCurrentClassMasterUseCase (which takes a Clazz id and returns masters across every
// section/stream of that class), this is scoped to one specific ClassSession - the teacher
// assignment page needs the master(s) of exactly the section/stream being assigned, not every
// session of the class.
@Service
@Transactional
@RequiredArgsConstructor
public class GetClassMasterBySessionUseCase {

    private final ClassMasterRepository classMasterRepo;
    private final ClassSessionRepository classSessionRepo;

    public List<ClassMasterDTO> execute(String schoolId, String sessionId) {
        classSessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        return classMasterRepo.findAllActiveBySessionIdAndSchoolId(sessionId, schoolId).stream()
                .map(ClassMasterMapper::toDTO)
                .toList();
    }
}
