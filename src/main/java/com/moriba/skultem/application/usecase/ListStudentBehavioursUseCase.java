package com.moriba.skultem.application.usecase;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BehaviourDTO;
import com.moriba.skultem.application.mapper.BehaviourMapper;
import com.moriba.skultem.domain.repository.BehaviourRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * One student's behaviour records - every class and year they were in, newest first - found by the student, never by
 * name (two students can share a name) and never limited to one class's first page.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ListStudentBehavioursUseCase {
    private final BehaviourRepository repo;

    public record Summary(long total, long positive, long negative) {
    }

    public Page<BehaviourDTO> execute(String schoolId, String studentId, int page, int size) {
        return repo.findByStudent(schoolId, studentId, PageRequest.of(Math.max(page, 0), Math.max(size, 1)))
                .map(BehaviourMapper::toDTO);
    }

    public Summary summary(String schoolId, String studentId) {
        Map<String, Long> counts = repo.countByKindForStudent(schoolId, studentId);
        long positive = counts.getOrDefault("POSITIVE", 0L);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new Summary(total, positive, total - positive);
    }
}
