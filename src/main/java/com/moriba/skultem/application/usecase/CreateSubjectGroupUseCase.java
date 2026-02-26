package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SubjectGroupDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SubjectGroupMapper;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;

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

    public SubjectGroupDTO execute(String schoolId, String name, String level, String classId, String streamId,
            boolean required, int minSelection, int maxSelection, int displayOrder) {
        if (minSelection > maxSelection) {
            throw new RuleException("minSelection cannot exceed maxSelection");
        }

        Clazz clazz = null;
        Stream stream = null;

        Level levelEnum = Level.valueOf(level);
        if (levelEnum == Level.SSS) {
            if (streamId == null || streamId.isBlank()) {
                throw new RuleException("stream is required for SSS subject group");
            }

            stream = streamRepo.findByIdAndSchoolId(streamId, schoolId).orElseThrow(() -> new RuleException("stream not found"));
            clazz = null;
        } else {
            if (classId == null || classId.isBlank()) {
                throw new RuleException("class is required for PRIMARY/JSS subject group");
            }
            clazz = classRepo.findByIdAndSchool(classId, schoolId).orElseThrow(() -> new RuleException("class not found"));
            
            stream = null;
        }

        var id = rg.generate("SUBJECT_GROUP", "SBG");
        var record = SubjectGroup.create(id, schoolId, name, levelEnum, clazz, stream, required, minSelection,
                maxSelection, displayOrder);
        repo.save(record);
        return SubjectGroupMapper.toDTO(record);
    }
}
