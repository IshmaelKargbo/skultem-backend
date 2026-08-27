package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassMasterDTO;
import com.moriba.skultem.application.dto.ClassMasterRecord;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ClassMasterMapper;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.model.ClassSession;
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
        private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

        public List<ClassMasterDTO> executeDTO(String schoolId, String classId, String academicYearId) {

                return getMasters(schoolId, classId, academicYearId).stream()
                                .map(ClassMasterMapper::toDTO)
                                .collect(Collectors.toList());
        }

        public List<ClassMasterRecord> executeRecord(String schoolId, String classId, String academicYearId) {
                return getMasters(schoolId, classId, academicYearId).stream()
                                .map(ClassMasterMapper::toRecord)
                                .collect(Collectors.toList());
        }

        private List<ClassMaster> getMasters(String schoolId, String classId, String academicYearId) {

                AcademicYear academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);

                List<ClassSession> sessions = classSessionRepo.findAllByClassIdAndAcademicYearIdAndSchoolId(
                                classId,
                                academicYear.getId(),
                                schoolId);

                if (sessions.isEmpty()) {
                        throw new NotFoundException(
                                        "No active class sessions found for this class");
                }

                return sessions.stream()
                                .map(session -> classMasterRepo
                                                .findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(
                                                                session.getId())
                                                .orElse(null))
                                .filter(master -> master != null)
                                .collect(Collectors.toList());
        }
}