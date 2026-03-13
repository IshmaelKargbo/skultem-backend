package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SubjectGroupMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@Transactional
@RequiredArgsConstructor
public class CreateSubjectGroupUseCase {

    private final SubjectGroupRepository repo;
    private final ClassRepository classRepo;
    private final StreamRepository streamRepo;
    private final ReferenceGeneratorUsecase rg;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SUBJECT_GROUP_CREATED")
    public SubjectGroupDTO execute(
            String schoolId,
            String name,
            String classId,
            String streamId,
            int totalSelection) {

        Clazz clazz = null;
        Stream stream = null;

        if (streamId != null && !streamId.isBlank()) {

            stream = streamRepo.findByIdAndSchoolId(streamId, schoolId)
                    .orElseThrow(() -> new RuleException("Stream not found"));
        }

        else if (classId != null && !classId.isBlank()) {

            clazz = classRepo.findByIdAndSchool(classId, schoolId)
                    .orElseThrow(() -> new RuleException("Class not found"));

            if (clazz.getLevel() == Level.PRIMARY) {
                throw new RuleException("Subject groups are not allowed for PRIMARY level");
            }

            if (clazz.getLevel() == Level.SSS) {
                throw new RuleException("For SSS classes, subject group must be assigned to a stream");
            }
        }

        else {
            throw new RuleException("Either class or stream must be provided");
        }

        var id = rg.generate("SUBJECT_GROUP", "SBG");

        var record = SubjectGroup.create(
                id,
                schoolId,
                name,
                clazz,
                stream,
                totalSelection
        );

        repo.save(record);

        String scopeName = stream != null ? stream.getName() : clazz.getName();
        logActivityUseCase.log(
                schoolId,
                ActivityType.SUBJECT,
                "New subject group created",
                record.getName() + " - " + scopeName,
                null,
                record.getId());

        return SubjectGroupMapper.toDTO(record);
    }
}
