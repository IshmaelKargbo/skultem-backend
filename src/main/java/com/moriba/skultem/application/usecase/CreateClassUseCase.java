package com.moriba.skultem.application.usecase;

import java.util.*;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.*;
import com.moriba.skultem.domain.repository.*;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateClassUseCase {

    // Which sections one stream runs (Art -> A, B). See execute().
    public record StreamSectionsInput(String streamId, List<String> sectionIds) {
    }

    private final ClassRepository classRepo;
    private final ClassSessionRepository sessionRepo;
    private final ClassSectionRepository classSectionRepo;
    private final ClassStreamRepository classStreamRepo;
    private final StreamRepository streamRepo;
    private final AcademicYearRepository academicYearRepo;
    private final SectionRepository sectionRepo;
    private final AssessmentTemplateRepository assessmentTemplateRepo;
    private final LogActivityUseCase logActivityUseCase;
    private final SchoolLevelRepository schoolLevelRepo;

    @AuditLogAnnotation(action = "CLASS_CREATED")
    public ClassDTO execute(
            String school,
            String name,
            Integer levelOrder,
            List<String> sectionIds,
            List<String> streamIds,
            String assessmentTemplateId,
            String level,
            List<StreamSectionsInput> streamSections) {

        // Check for duplicates
        if (classRepo.existsByNameAndSchool(name, school)) {
            throw new AlreadyExistsException("Class with session '" + name + "' already exists in this school.");
        }
        // The order is only a position in lists now - not something a user has to pick - so it
        // defaults to after the school's last class. Only an explicitly requested one can clash.
        if (levelOrder != null && classRepo.existsByLevelOrderAndSchool(levelOrder, school)) {
            throw new AlreadyExistsException(
                    "Class with level order '" + levelOrder + "' already exists in this school.");
        }
        int order = levelOrder != null ? levelOrder : classRepo.maxLevelOrderBySchool(school) + 1;

        // Fetch active academic year
        AcademicYear academicYear = academicYearRepo.findActiveBySchool(school)
                .orElseThrow(() -> new IllegalStateException("No active academic year found for school: " + school));

        // Create Class
        Level levelEnum = Level.valueOf(level.toUpperCase());
        // Only levels the school has said it offers (Settings > School Structure).
        boolean offered = schoolLevelRepo.findBySchoolId(school).stream()
                .anyMatch(l -> l.getLevel() == levelEnum);
        if (!offered) {
            throw new RuleException(levelEnum.getLabel()
                    + " isn't one of this school's levels. Add it under Settings > School Structure first.");
        }
        String classId = UUID.randomUUID().toString();
        AssessmentTemplate template = null;
        if (assessmentTemplateId != null && !assessmentTemplateId.isBlank()) {
            template = assessmentTemplateRepo.findByIdAndSchoolId(assessmentTemplateId, school)
                    .orElseThrow(() -> new NotFoundException("Assessment template not found"));
        }

        Clazz clazz = Clazz.create(classId, school, template, name, levelEnum, order);
        classRepo.save(clazz);

        // A streamed class can run different sections per stream (Art -> A, B; Science -> A). When
        // that's given it decides both which streams the class has and which sections exist at all;
        // otherwise every chosen section runs under every chosen stream, as before.
        boolean perStream = levelEnum.isStreamed() && streamSections != null && !streamSections.isEmpty();
        if (perStream) {
            var seen = new HashSet<String>();
            for (var pair : streamSections) {
                if (!seen.add(pair.streamId())) {
                    throw new RuleException("A stream can only be listed once");
                }
            }
            streamIds = streamSections.stream().map(StreamSectionsInput::streamId).toList();
            sectionIds = streamSections.stream().flatMap(p -> p.sectionIds().stream()).distinct().toList();
        }

        // Fetch Sections once
        List<Section> sections = sectionIds.stream()
                .map(id -> sectionRepo.findByIdAndSchoolId(id, school)
                        .orElseThrow(() -> new NotFoundException("Section not found: " + id)))
                .toList();
        Map<String, Section> sectionById = new HashMap<>();
        sections.forEach(s -> sectionById.put(s.getId(), s));

        // Fetch Streams once (for SSS)
        List<Stream> streams = Collections.emptyList();
        if (levelEnum.isStreamed() && streamIds != null && !streamIds.isEmpty()) {
            streams = streamIds.stream()
                    .map(id -> streamRepo.findByIdAndSchoolId(id, school)
                            .orElseThrow(() -> new NotFoundException("Stream not found: " + id)))
                    .toList();
        }

        // Link Sections to Class
        for (Section section : sections) {
            if (!classSectionRepo.existsByClassIdAndSchoolIdAndSectionId(classId, school, section.getId())) {
                String csId = UUID.randomUUID().toString();
                ClassSection classSection = ClassSection.create(csId, school, clazz, section);
                classSectionRepo.save(classSection);
            }
        }

        // Link Streams to Class (SSS only)
        for (Stream stream : streams) {
            if (!classStreamRepo.existsByClassIdAndSchoolIdAndStreamId(classId, school, stream.getId())) {
                String cstId = UUID.randomUUID().toString();
                ClassStream classStream = ClassStream.create(cstId, school, stream, clazz);
                classStreamRepo.save(classStream);
            }
        }

        // Create Class Sessions
        List<ClassSession> sessionsToSave = new ArrayList<>();
        if (perStream) {
            for (Stream stream : streams) {
                var pair = streamSections.stream().filter(p -> p.streamId().equals(stream.getId())).findFirst().orElseThrow();
                for (String sectionId : pair.sectionIds().stream().distinct().toList()) {
                    Section section = sectionById.get(sectionId);
                    if (!sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                            classId, academicYear.getId(), section.getId(), stream.getId(), school)) {
                        sessionsToSave.add(ClassSession.create(UUID.randomUUID().toString(), school, clazz, stream,
                                section, academicYear));
                    }
                }
            }
        }
        for (Section section : perStream ? List.<Section>of() : sections) {

            if (levelEnum.isStreamed()) {
                for (Stream stream : streams) {
                    if (!sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                            classId, academicYear.getId(), section.getId(), stream.getId(), school)) {
                        sessionsToSave.add(ClassSession.create(UUID.randomUUID().toString(), school, clazz, stream,
                                section, academicYear));
                    }
                }
            } else {
                if (!sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(
                        classId, academicYear.getId(), section.getId(), school)) {
                    sessionsToSave.add(ClassSession.create(
                            UUID.randomUUID().toString(),
                            school, clazz, null, section, academicYear));
                }
            }
        }

        sessionRepo.saveAll(sessionsToSave);

        logActivityUseCase.log(
                school,
                ActivityType.CLASS,
                "New class created",
                clazz.getName() + " (" + clazz.getLevel().name() + ")",
                null,
                clazz.getId());

        return ClassMapper.toDTO(clazz);
    }
}
