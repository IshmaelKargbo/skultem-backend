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

                Map<String, TeacherSubject> assignedMap = assignedList.stream()
                                .filter(ts -> ts.getSubject() != null && ts.getSubject().getId() != null)
                                .collect(Collectors.toMap(
                                                ts -> ts.getSubject().getId(),
                                                ts -> ts,
                                                (a, b) -> a // prevent duplicate key crash
                                ));

                List<TeacherSubjectDTO> dtoList = subjectsById.entrySet().stream()
                                .map(entry -> {
                                        String subjectId = entry.getKey();
                                        var existing = assignedMap.get(subjectId);
                                        var section = session.getSection();

                                        if (existing != null) {
                                                return TeacherSubjectMapper.toDTO(existing);
                                        }

                                        String streamName = session.getStream() != null ? session.getStream().getName()
                                                        : "";
                                        String streamId = session.getStream() != null ? session.getStream().getId()
                                                        : "";

                                        return new TeacherSubjectDTO(null, clazz.getName(), sessionId, clazz.getId(),
                                                        section.getName(), section.getId(), streamName, streamId, null,
                                                        null, entry.getValue(), entry.getKey(), null, null, null);
                                })
                                .toList();

                return new PageImpl<>(dtoList, pageable, dtoList.size());
        }
}