package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateClassSessionsForAcademicYearUseCase {

    private final ClassRepository classRepo;
    private final ClassSectionRepository sectionRepo;
    private final ClassStreamRepository streamRepo;
    private final ClassSessionRepository sessionRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ReferenceGeneratorUsecase rg;
    private final CarryForwardClassSessionSetupUseCase carryForwardClassSessionSetupUseCase;

    public int execute(String schoolId, String academicYearId) {
        var academicYear = academicYearRepo.findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        var classes = classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent();

        int created = 0;

        for (var clazz : classes) {
            var sections = sectionRepo.findByClassIdAndSchoolId(clazz.getId(), schoolId);
            if (sections.isEmpty()) {
                continue;
            }

            if (clazz.getLevel() == Level.SSS) {
                var streams = streamRepo.findAllByClassIdAndSchoolId(clazz.getId(), schoolId);

                for (var section : sections) {
                    for (var classStream : streams) {
                        var exists = sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                                clazz.getId(), academicYear.getId(), section.getSection().getId(),
                                classStream.getStream().getId(), schoolId);

                        if (exists) {
                            continue;
                        }

                        var session = ClassSession.create(rg.generate("CLASS_SESSION", "CSN"), schoolId, clazz,
                                classStream.getStream(), section.getSection(), academicYear);
                        sessionRepo.save(session);
                        carryForwardClassSessionSetupUseCase.execute(session);
                        created++;
                    }
                }

                continue;
            }

            for (var section : sections) {
                var exists = sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(
                        clazz.getId(), academicYear.getId(), section.getSection().getId(), schoolId);

                if (exists) {
                    continue;
                }

                var session = ClassSession.create(rg.generate("CLASS_SESSION", "CSN"), schoolId, clazz, null,
                        section.getSection(), academicYear);
                sessionRepo.save(session);
                carryForwardClassSessionSetupUseCase.execute(session);
                created++;
            }
        }

        return created;
    }
}
