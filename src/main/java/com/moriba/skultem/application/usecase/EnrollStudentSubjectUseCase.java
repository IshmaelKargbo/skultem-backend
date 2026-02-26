package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollStudentSubjectUseCase {
    private final StreamRepository streamRepo;
    private final SubjectRepository subjectRepo;
    private final SubjectGroupRepository groupRepo;
    private final StreamSubjectRepository streamSubjectRepo;
    private final ReferenceGeneratorUsecase rg;

    public void execute(String schoolId, String streamId, List<SubjectAssignment> assignments) {
        var stream = streamRepo.findByIdAndSchoolId(streamId, streamId).orElseThrow(() -> new RuleException("stream not found"));

        for (SubjectAssignment assignment : assignments) {
            var subject = subjectRepo.findById(assignment.subjectId())
                    .orElseThrow(() -> new RuleException("subject not found"));
            SubjectGroup group = null;
            if (!subject.getSchoolId().equals(schoolId)) {
                throw new RuleException("subject not in school");
            }

            if (assignment.subjectGroupId() != null && !assignment.subjectGroupId().isBlank()) {
                group = groupRepo.findByIdAndStreamAndSchoolId(assignment.subjectGroupId(), streamId, schoolId)
                        .orElseThrow(() -> new RuleException("subject group not found"));
            }
            if (streamSubjectRepo.existsByStreamAndSubject(streamId, assignment.subjectId())) {
                continue;
            }
            var id = rg.generate("STREAM_SUBJECT", "SSJ");
            var record = StreamSubject.create(id, schoolId, stream, subject, group, assignment.mandatory());
            streamSubjectRepo.save(record);
        }
    }

    public record SubjectAssignment(String subjectId, String subjectGroupId, boolean mandatory) {
    }
}
