package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassSectionMapper;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.SectionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSectionByClassUseCase {

    private final ClassSectionRepository repo;
    private final SectionRepository sectionRepo;

    public List<ClassSectionDTO> execute(String schoolId, String classId) {

        List<ClassSection> classSections =
                repo.findByClassIdAndSchoolId(classId, schoolId);

        return classSections.stream()
                .map(classSection -> {

                    Section section = sectionRepo.findById(classSection.getSection().getId())
                            .orElseThrow(() ->
                                    new NotFoundException("Section not found"));

                    return ClassSectionMapper.toDTO(classSection, section.getName());
                })
                .toList();
    }
}
