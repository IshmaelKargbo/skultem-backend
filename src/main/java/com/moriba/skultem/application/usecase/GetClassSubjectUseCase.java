package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ClassSubjectResponse;
import com.moriba.skultem.application.dto.OptionGroupResponse;
import com.moriba.skultem.application.dto.SubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.SubjectMapper;
import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.model.vo.Level;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GetClassSubjectUseCase {

        private final ClassSubjectRepository classSubjectRepo;
        private final StreamSubjectRepository streamSubjectRepo;
        private final ClassStreamRepository classStreamRepo;
        private final ClassRepository classRepo;

        public ClassSubjectResponse execute(String schoolId, String classId, String streamId) {
                var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                if (clazz.getLevel().equals(Level.SSS)) {
                        if (streamId == null || streamId.isBlank()) {
                                throw new RuleException("Stream is required for SSS class");
                        }
                        if (!classStreamRepo.existsByClassIdAndSchoolIdAndStreamId(classId, schoolId, streamId)) {
                                throw new RuleException("Stream does not belong to this class");
                        }
                        return getStreamSubjects(schoolId, streamId);
                }

                return getClassSubjects(schoolId, classId);
        }

        private ClassSubjectResponse getClassSubjects(String schoolId, String classId) {
                var subjects = classSubjectRepo
                                .findAllByClassIdAndSchoolId(classId, schoolId, Pageable.unpaged())
                                .getContent();
                List<SubjectDTO> coreSubjects = subjects.stream()
                                .filter(cs -> Boolean.TRUE.equals(cs.getMandatory()))
                                .sorted(Comparator.comparing(cs -> cs.getSubject().getName()))
                                .map(cs -> SubjectMapper.toDTO(cs.getSubject()))
                                .toList();

                Map<String, List<ClassSubject>> grouped = subjects.stream()
                                .filter(cs -> Boolean.FALSE.equals(cs.getMandatory()))
                                .collect(Collectors.groupingBy(
                                                cs -> cs.getGroup().getId()));

                List<OptionGroupResponse> options = new ArrayList<>();

                for (var entry : grouped.entrySet()) {

                        List<ClassSubject> groupSubjects = entry.getValue();

                        SubjectGroup group = groupSubjects.get(0).getGroup();

                        List<SubjectDTO> optionList = groupSubjects.stream()
                                        .sorted(Comparator.comparing(cs -> cs.getSubject().getName()))
                                        .map(cs -> SubjectMapper.toDTO(cs.getSubject()))
                                        .toList();

                        options.add(new OptionGroupResponse(
                                        group.getId(),
                                        group.getName(),
                                        group.getTotalSelection(),
                                        optionList));
                }

                return new ClassSubjectResponse(coreSubjects, options);
        }

        private ClassSubjectResponse getStreamSubjects(String schoolId, String streamId) {
                var subjects = streamSubjectRepo
                                .findAllByStreamIdAndSchoolId(streamId, schoolId, Pageable.unpaged())
                                .getContent();

                List<SubjectDTO> coreSubjects = subjects.stream()
                                .filter(ss -> Boolean.TRUE.equals(ss.getMandatory()))
                                .sorted(Comparator.comparing(ss -> ss.getSubject().getName()))
                                .map(ss -> SubjectMapper.toDTO(ss.getSubject()))
                                .toList();

                Map<String, List<StreamSubject>> grouped = subjects.stream()
                                .filter(ss -> Boolean.FALSE.equals(ss.getMandatory()))
                                .filter(ss -> ss.getGroup() != null)
                                .collect(Collectors.groupingBy(ss -> ss.getGroup().getId()));

                List<OptionGroupResponse> options = new ArrayList<>();
                for (var entry : grouped.entrySet()) {
                        List<StreamSubject> groupSubjects = entry.getValue();
                        SubjectGroup group = groupSubjects.get(0).getGroup();
                        List<SubjectDTO> optionList = groupSubjects.stream()
                                        .sorted(Comparator.comparing(ss -> ss.getSubject().getName()))
                                        .map(ss -> SubjectMapper.toDTO(ss.getSubject()))
                                        .toList();
                        options.add(new OptionGroupResponse(
                                        group.getId(),
                                        group.getName(),
                                        group.getTotalSelection(),
                                        optionList));
                }

                return new ClassSubjectResponse(coreSubjects, options);
        }
}
