package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RemoveTeacherFromClassUseCase {

    private final ClassMasterRepository repo;
    private final ClassSessionRepository sessionRepo;
    private final AcademicYearRepository academicYearRepo;

    @AuditLogAnnotation(action = "TEACHER_REMOVED")
    public void execute(String id, String schoolId) {

        var record = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Class master not found"));

        if (!record.isActive()) {
            throw new RuleException("Class master assignment already ended");
        }

        var session = sessionRepo
                .findByIdAndSchoolId(record.getSession().getId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Class session not found"));

        var academicYear = academicYearRepo
                .findByIdAndSchoolId(session.getAcademicYear().getId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Academic year not found"));

        if (academicYear.isLocked()) {
            throw new RuleException("Cannot modify class master in a closed academic year");
        }

        record.end();
        repo.save(record);
    }
}

