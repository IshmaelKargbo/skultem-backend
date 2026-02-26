package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassStreamDTO;
import com.moriba.skultem.application.mapper.ClassStreamMapper;
import com.moriba.skultem.domain.repository.ClassStreamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassStreamByIdUseCase {

    private final ClassStreamRepository repo;

    public List<ClassStreamDTO> execute(String school, String classId) {
        return repo.findAllByClassIdAndSchoolId(classId, school).stream().map(ClassStreamMapper::toDTO).toList();
    }
}
