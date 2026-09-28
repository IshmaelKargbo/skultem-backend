package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassDTO;
import com.moriba.skultem.application.error.AlreadyExistsException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.ClassMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.model.AssessmentTemplate;
import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Section;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SectionRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateClassUseCase {

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
    public ClassDTO execute(String school, String name, Integer levelOrder, List<String> sectionIds, List<String> streamIds, String assessmentTemplateId, String level, List<StreamSectionsInput> streamSections) {

        Level levelEnum;
        try {
            levelEnum = Level.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuleException("Invalid class level: " + level);
        }

        boolean offered = schoolLevelRepo.findBySchoolId(school).stream().anyMatch(l -> l.getLevel() == levelEnum);
        if (!offered) {
            throw new RuleException(levelEnum.getLabel() + " isn't one of this school's levels. " + "Add it under Settings > School Structure first.");
        }

        AcademicYear academicYear = academicYearRepo.findActiveBySchool(school).orElseThrow(() -> new IllegalStateException("No active academic year found for school: " + school));
        Clazz clazz = classRepo.findByNameAndSchool(name, school).orElse(null);

        boolean classCreated = false;
        if (clazz == null) {
            int order;
            if (levelOrder != null) {
                if (classRepo.existsByLevelOrderAndSchool(levelOrder, school)) {
                    throw new AlreadyExistsException("Class with level order '" + levelOrder + "' already exists in this school.");
                }
                order = levelOrder;
            } else {
                order = classRepo.maxLevelOrderBySchool(school) + 1;
            }

            AssessmentTemplate template = null;
            if (assessmentTemplateId != null && !assessmentTemplateId.isBlank()) {
                template = assessmentTemplateRepo.findByIdAndSchoolId(assessmentTemplateId, school).orElseThrow(() -> new NotFoundException("Assessment template not found"));
            }
            String classId = UUID.randomUUID().toString();
            clazz = Clazz.create(classId, school, template, name, levelEnum, order);
            classRepo.save(clazz);
            classCreated = true;
        } else {
            if (clazz.getLevel() != levelEnum) {
                throw new RuleException("Class '" + name + "' already exists with level '" + clazz.getLevel().getLabel() + "', not '" + levelEnum.getLabel() + "'.");
            }
        }

        boolean perStream = levelEnum.isStreamed() && streamSections != null && !streamSections.isEmpty();
        if (perStream) {
            validateStreamSections(streamSections);
            streamIds = streamSections.stream().map(StreamSectionsInput::streamId).distinct().toList();
            sectionIds = streamSections.stream().flatMap(input -> input.sectionIds() == null ? java.util.stream.Stream.empty() : input.sectionIds().stream()).distinct().toList();
        }

        List<Section> sections = sectionIds == null ? Collections.emptyList() : sectionIds.stream()
                .distinct().map(id -> sectionRepo.findByIdAndSchoolId(id, school)
                .orElseThrow(() -> new NotFoundException("Section not found: " + id)))
                .toList();

        Map<String, Section> sectionById = new HashMap<>();
        sections.forEach(section -> sectionById.put(section.getId(), section));

        List<Stream> streams = Collections.emptyList();
        if (levelEnum.isStreamed() && streamIds != null && !streamIds.isEmpty()) {
            streams = streamIds.stream().distinct().map(id -> streamRepo.findByIdAndSchoolId(id, school).orElseThrow(() -> new NotFoundException("Stream not found: " + id))).toList();
        }

        createSections(clazz.getId(), school, clazz, sections);
        if (levelEnum.isStreamed()) {
            createStreams(clazz.getId(), school, clazz, streams);
        }

        List<ClassSession> sessionsToSave = new ArrayList<>();
        if (levelEnum.isStreamed()) {
            if (perStream) {
                for (StreamSectionsInput input : streamSections) {
                    Stream stream = streams.stream().filter(s -> s.getId().equals(input.streamId())).findFirst().orElseThrow(() -> new NotFoundException("Stream not found: " + input.streamId()));
                    if (input.sectionIds() == null || input.sectionIds().isEmpty()) {
                        throw new RuleException("At least one section must be selected for stream '" + stream.getName() + "'.");
                    }
                    for (String sectionId : input.sectionIds().stream().distinct().toList()) {
                        Section section = sectionById.get(sectionId);
                        if (section == null) {
                            throw new NotFoundException("Section not found: " + sectionId);
                        }
                        addSessionIfMissing(sessionsToSave, clazz, school, academicYear, section, stream);
                    }
                }
            } else {
                for (Stream stream : streams) {
                    for (Section section : sections) {
                        addSessionIfMissing(sessionsToSave, clazz, school, academicYear, section, stream);
                    }
                }
            }
        } else {
            for (Section section : sections) {
                addSessionIfMissing(sessionsToSave, clazz, school, academicYear, section, null);
            }
        }

        if (sessionsToSave.isEmpty()) {
            throw new AlreadyExistsException(buildDuplicateMessage(name, levelEnum, sections, streams, perStream));
        }

        sessionRepo.saveAll(sessionsToSave);
        String actionMessage = classCreated ? "New class created" : "Class configuration updated";
        String description = classCreated ? clazz.getName() + " (" + clazz.getLevel().name() + ")" : clazz.getName() + " (" + clazz.getLevel().name() + ") - added " + sessionsToSave.size() + " new class session(s)";
        logActivityUseCase.log(school, ActivityType.CLASS, actionMessage, description, null, clazz.getId());
        return ClassMapper.toDTO(clazz);
    }

    private void validateStreamSections(List<StreamSectionsInput> streamSections) {
        HashSet<String> seenStreams = new HashSet<>();
        for (StreamSectionsInput input : streamSections) {
            if (input.streamId() == null || input.streamId().isBlank()) {
                throw new RuleException("Stream ID cannot be empty.");
            }
            if (!seenStreams.add(input.streamId())) {
                throw new RuleException("A stream can only be listed once.");
            }
            if (input.sectionIds() == null || input.sectionIds().isEmpty()) {
                throw new RuleException("A stream must have at least one section.");
            }
        }
    }

    private void addSessionIfMissing(List<ClassSession> sessionsToSave, Clazz clazz, String school, AcademicYear academicYear, Section section, Stream stream) {
        boolean exists;
        if (stream != null) {
            exists = sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(clazz.getId(), academicYear.getId(), section.getId(), stream.getId(), school);
        } else {
            exists = sessionRepo.existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(clazz.getId(), academicYear.getId(), section.getId(), school);
        }
        if (exists) {
            return;
        }

        boolean alreadyQueued = sessionsToSave.stream().anyMatch(session -> sameSessionCombination(session, section, stream, academicYear));
        if (alreadyQueued) {
            return;
        }
        sessionsToSave.add(ClassSession.create(UUID.randomUUID().toString(), school, clazz, stream, section, academicYear));
    }

    private boolean sameSessionCombination(ClassSession session, Section section, Stream stream, AcademicYear academicYear) {
        if (!session.getAcademicYear().getId().equals(academicYear.getId())) {
            return false;
        }
        if (!session.getSection().getId().equals(section.getId())) {
            return false;
        }
        if (session.getStream() == null && stream == null) {
            return true;
        }
        if (session.getStream() == null || stream == null) {
            return false;
        }
        return session.getStream().getId().equals(stream.getId());
    }

    private void createSections(String classId, String school, Clazz clazz, List<Section> sections) {
        for (Section section : sections) {
            if (!classSectionRepo.existsByClassIdAndSchoolIdAndSectionId(classId, school, section.getId())) {
                String classSectionId = UUID.randomUUID().toString();
                ClassSection classSection = ClassSection.create(classSectionId, school, clazz, section);
                classSectionRepo.save(classSection);
            }
        }
    }

    private void createStreams(String classId, String school, Clazz clazz, List<Stream> streams) {
        for (Stream stream : streams) {
            if (!classStreamRepo.existsByClassIdAndSchoolIdAndStreamId(classId, school, stream.getId())) {
                String classStreamId = UUID.randomUUID().toString();
                var classStream = com.moriba.skultem.domain.model.ClassStream.create(classStreamId, school, stream, clazz);
                classStreamRepo.save(classStream);
            }
        }
    }

    private String buildDuplicateMessage(String className, Level level, List<Section> sections, List<Stream> streams, boolean perStream) {
        if (!level.isStreamed()) {
            String sectionNames = sections.stream().map(Section::getName).toList().toString();
            return "Class '" + className + "' already has the requested section(s): " + sectionNames;
        }
        if (perStream) {
            return "Class '" + className + "' already has all the requested stream and section combinations.";
        }

        String streamNames = streams.stream().map(Stream::getName).toList().toString();
        return "Class '" + className + "' already has the requested section and stream combinations for streams: " + streamNames;
    }
}
