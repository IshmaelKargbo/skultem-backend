package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Marks a class as terminal (graduating) or not - a terminal class (e.g. Class 6, JSS 3, SSS 3)
 * has no next class, so a PROMOTE outcome for it graduates the student out of the school instead
 * of moving them up a class.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateClassTerminalUseCase {

    private final ClassRepository repo;

    public ClassDTO execute(String schoolId, String classId, boolean terminal) {
        var clazz = repo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        clazz.setTerminal(terminal);
        if (terminal) {
            // A graduating class has nowhere to promote into.
            clazz.setNextClass(null);
        }
        repo.save(clazz);

        return ClassMapper.toDTO(clazz);
    }
}
