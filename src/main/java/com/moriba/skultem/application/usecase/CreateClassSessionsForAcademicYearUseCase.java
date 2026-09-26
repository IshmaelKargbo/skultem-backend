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

            if (clazz.getLevel().isStreamed()) {
                var streams = streamRepo.findAllByClassIdAndSchoolId(clazz.getId(), schoolId);

                // A streamed class may run different sections per stream (Art -> A, B; Science -> A), so
                // repeat the pairing the class has already used rather than every section x every
                // stream. A class that has never had a session falls back to all combinations.
                var usedPairs = sessionRepo.findAllByClassIdAndSchoolId(clazz.getId(), schoolId).stream()
                        .filter(s -> s.getStream() != null && s.getSection() != null)
                        .map(s -> s.getSection().getId() + "|" + s.getStream().getId())
                        .collect(java.util.stream.Collectors.toSet());

                for (var section : sections) {
                    for (var classStream : streams) {
                        if (!usedPairs.isEmpty()
                                && !usedPairs.contains(section.getSection().getId() + "|" + classStream.getStream().getId())) {
                            continue;
                        }
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
