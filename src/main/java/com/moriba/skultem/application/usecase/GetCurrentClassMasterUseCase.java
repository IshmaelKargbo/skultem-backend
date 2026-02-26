package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMasterMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class GetCurrentClassMasterUseCase {

    private final ClassMasterRepository classMasterRepo;
    private final ClassSessionRepository classSessionRepo;
    private final AcademicYearRepository academicYearRepo;

    public List<ClassMasterDTO> execute(String schoolId, String classId) {

        AcademicYear academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() ->
                        new NotFoundException("No active academic year found"));

        List<ClassSession> sessions =
                classSessionRepo.findAllByClassIdAndAcademicYearIdAndSchoolId(
                        classId,
                        academicYear.getId(),
                        schoolId
                );

        if (sessions.isEmpty()) {
            throw new NotFoundException(
                    "No active class sessions found for this class");
        }

        return sessions.stream()
                .map(session ->
                        classMasterRepo
                                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(
                                        session.getId()
                                )
                                .orElse(null)
                )
                .filter(master -> master != null)
                .map(ClassMasterMapper::toDTO)
                .collect(Collectors.toList());
    }
}