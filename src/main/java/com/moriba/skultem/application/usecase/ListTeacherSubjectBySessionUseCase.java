package com.moriba.skultem.application.usecase;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TeacherSubjectDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.TeacherSubjectMapper;
import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListTeacherSubjectBySessionUseCase {

        private final TeacherSubjectRepository teacherSubjectRepo;
        private final ClassSessionRepository sessionRepo;
        private final ClassSubjectRepository classSubjectRepo;
        private final StreamSubjectRepository streamSubjectRepo;

        public Page<TeacherSubjectDTO> execute(String schoolId, String sessionId, int page, int size) {

                Pageable pageable = size > 0
                                ? PageRequest.of(page, size)
                                : Pageable.unpaged();

                var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                                .orElseThrow(() -> new NotFoundException("Class session not found"));

                var clazz = session.getClazz();

                LinkedHashMap<String, String> subjectsById = new LinkedHashMap<>();

                if (clazz.getLevel().equals(Level.SSS) && session.getStream() != null) {
                        streamSubjectRepo
                                        .findAllByStreamIdAndSchoolId(session.getStream().getId(), schoolId,
                                                        Pageable.unpaged())
                                        .forEach(s -> subjectsById.put(
                                                        s.getSubject().getId(),
                                                        s.getSubject().getName()));
                } else {
                        classSubjectRepo
                                        .findAllByClassIdAndSchoolId(clazz.getId(), schoolId, Pageable.unpaged())
                                        .forEach(s -> subjectsById.put(
                                                        s.getSubject().getId(),
                                                        s.getSubject().getName()));
                }

                var assignedList = teacherSubjectRepo
                                .findByClassSessionIdAndSchoolId(sessionId, schoolId, Pageable.unpaged());

                // A subject can now have more than one teacher assigned (e.g. co-taught classes where
                // any of them can handle it) - group instead of collapsing to a single entry, or a
                // second/third teacher assigned to the same subject would silently disappear from this
                // list and then get deleted the next time assignments are saved (AssignSubjectToTeacherUseCase
                // removes anything not present in what it's handed back).
                Map<String, List<TeacherSubject>> assignedBySubject = assignedList.stream()
                                .filter(ts -> ts.getSubject() != null && ts.getSubject().getId() != null)
                                .collect(Collectors.groupingBy(ts -> ts.getSubject().getId()));

                List<TeacherSubjectDTO> dtoList = new ArrayList<>();

                for (var entry : subjectsById.entrySet()) {
                        String subjectId = entry.getKey();
                        var existingForSubject = assignedBySubject.get(subjectId);

                        if (existingForSubject != null && !existingForSubject.isEmpty()) {
                                existingForSubject.stream().map(TeacherSubjectMapper::toDTO).forEach(dtoList::add);
                                continue;
                        }

                        var section = session.getSection();
                        String streamName = session.getStream() != null ? session.getStream().getName() : "";
                        String streamId = session.getStream() != null ? session.getStream().getId() : "";

                        dtoList.add(new TeacherSubjectDTO(null, clazz.getName(), sessionId, clazz.getId(),
                                        section.getName(), section.getId(), streamName, streamId, null,
                                        null, entry.getValue(), entry.getKey(), null, null, null));
                }

                return new PageImpl<>(dtoList, pageable, dtoList.size());
        }
}