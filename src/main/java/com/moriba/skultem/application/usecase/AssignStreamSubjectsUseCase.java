package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.Subject;
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
public class AssignStreamSubjectsUseCase {

        private final StreamRepository streamRepo;
        private final SubjectRepository subjectRepo;
        private final SubjectGroupRepository groupRepo;
        private final StreamSubjectRepository repo;
        private final ReferenceGeneratorUsecase rg;

        public void execute(String schoolId, String streamId, List<SubjectAssignment> assignments) {

                Stream stream = streamRepo.findByIdAndSchoolId(streamId, schoolId)
                                .orElseThrow(() -> new RuleException("Stream not found"));

                assignments = assignments == null ? List.of()
                                : assignments.stream()
                                                .filter(a -> a.subjectId() != null && !a.subjectId().isBlank())
                                                .toList();

                var existing = repo.findAllByStreamIdAndSchoolId(streamId, schoolId, Pageable.unpaged())
                                .getContent();

                var existingMap = existing.stream()
                                .collect(Collectors.toMap(
                                                ss -> ss.getSubject().getId(),
                                                ss -> ss,
                                                (a, b) -> {
                                                        throw new RuleException(
                                                                        "Duplicate stream subject records detected. Please clean up and retry.");
                                                }));

                var incomingSubjectIds = assignments.stream()
                                .map(SubjectAssignment::subjectId)
                                .collect(Collectors.toSet());

                if (incomingSubjectIds.size() != assignments.size()) {
                        throw new RuleException("Duplicate subjects detected.");
                }

                var subjects = incomingSubjectIds.isEmpty()
                                ? List.<Subject>of()
                                : subjectRepo.findAllByIdInAndSchoolId(incomingSubjectIds, schoolId);

                if (subjects.size() != incomingSubjectIds.size()) {
                        throw new RuleException("Invalid subject in request");
                }

                var subjectMap = subjects.stream()
                                                .collect(Collectors.toMap(s -> s.getId(), s -> s, (a, b) -> a));

                var groupIds = assignments.stream()
                                .map(SubjectAssignment::subjectGroupId)
                                .filter(id -> id != null && !id.isBlank())
                                .collect(Collectors.toSet());

                var groups = groupIds.isEmpty()
                                ? List.<SubjectGroup>of()
                                : groupRepo.findAllByIdInAndStreamAndSchoolId(groupIds, streamId, schoolId);

                if (groups.size() != groupIds.size()) {
                        throw new RuleException("Invalid subject group");
                }

                var groupMap = groups.stream()
                                .collect(Collectors.toMap(g -> g.getId(), g -> g));

                for (var request : assignments) {

                        var subjectId = request.subjectId();

                        var subject = subjectMap.get(subjectId);

                        SubjectGroup group = null;
                        if (request.subjectGroupId() != null && !request.subjectGroupId().isBlank()) {
                                group = groupMap.get(request.subjectGroupId());
                                if (group == null) {
                                        throw new RuleException("Invalid subject group");
                                }
                        }

                        if (existingMap.containsKey(subjectId)) {
                                existingMap.get(subjectId)
                                                .update(group, request.mandatory());
                                repo.save(existingMap.get(subjectId));
                        } else {
                                var id = rg.generate("STREAM_SUBJECT", "SSJ");
                                var record = StreamSubject.create(id, schoolId, stream,
                                                subject, group, request.mandatory());
                                repo.save(record);
                        }
                }

                var toDelete = existing.stream()
                                .filter(ss -> !incomingSubjectIds.contains(ss.getSubject().getId()))
                                .toList();

                for (var streamSubject : toDelete) {
                        repo.delete(streamSubject);
                }
        }

        public record SubjectAssignment(
                        String subjectId,
                        String subjectGroupId,
                        boolean mandatory) {
        }
}
