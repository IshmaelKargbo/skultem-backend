package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSectionDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassSectionMapper;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SectionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListClassSectionByClassUseCase {

    private final ClassSectionRepository repo;
    private final ClassSessionRepository sessionRepo;
    private final SectionRepository sectionRepo;

    public List<ClassSectionDTO> execute(String schoolId, String classId, String streamId, String academicYearId) {
        return getSectionDTOs(classId, schoolId, streamId, academicYearId);
    }

    public List<ClassSectionDTO> execute(String schoolId, String classId) {
        return fromSections(classId, schoolId);
    }

    private List<ClassSectionDTO> getSectionDTOs(String classId, String schoolId, String streamId, String academicYearId) {
        if (streamId == null) {
            return fromSections(classId, schoolId);
        }

        return fromSessions(classId, streamId, academicYearId);
    }

    private List<ClassSectionDTO> fromSections(String classId, String schoolId) {
        return repo.findByClassIdAndSchoolId(classId, schoolId).stream()
                .map(classSection -> {
                    Section section = sectionRepo.findById(classSection.getSection().getId())
                            .orElseThrow(()
                                    -> new NotFoundException("Section not found"));

                    return ClassSectionMapper.toDTO(classSection, section.getName());
                })
                .toList();
    }

    private List<ClassSectionDTO> fromSessions(String classId, String streamId, String academicYearId) {
        return sessionRepo.findAllByClassIdAndStreamIdAndAcademicYearId(classId, streamId, academicYearId).stream()
                .map(e -> {
                    return ClassSectionMapper.toDTO(e.getClazz(), e.getSection());
                })
                .toList();
    }
}
