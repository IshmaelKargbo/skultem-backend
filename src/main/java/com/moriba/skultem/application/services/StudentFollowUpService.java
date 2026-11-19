package com.moriba.skultem.application.services;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.StudentFollowUp;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.StudentFollowUpRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

// The follow-up log behind "needs attention": a teacher records what they did about a flagged pupil
// (called the guardian, spoke with the pupil...) and whether it's resolved. Who may read or record it is
// decided at the controller - the class's own teachers and management, as for the class roster.
@Service
@RequiredArgsConstructor
public class StudentFollowUpService {

    public static final int MAX_NOTE = 1000;

    private final StudentFollowUpRepository repo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    public record FollowUpDTO(String id, String studentId, String action, String note, boolean resolved,
            String recordedBy, Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public List<FollowUpDTO> listForClass(String schoolId, String classId) {
        return repo.findAllByClass(schoolId, classId).stream().map(StudentFollowUpService::toDTO).toList();
    }

    @Transactional
    public FollowUpDTO record(String schoolId, String classId, String studentId, String action, String note,
            boolean resolved, String userId) {
        if (studentId == null || studentId.isBlank()) {
            throw new BadRequestException("Choose the pupil this follow-up is about");
        }
        StudentFollowUp.Action parsed;
        try {
            parsed = StudentFollowUp.Action.valueOf(String.valueOf(action));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Say what was done: called, messaged or met the guardian, spoke with the "
                    + "pupil, or other");
        }
        String text = note == null ? null : note.trim();
        if (text != null && text.isEmpty()) {
            text = null;
        }
        if (parsed == StudentFollowUp.Action.OTHER && text == null) {
            throw new BadRequestException("Add a note saying what was done");
        }
        if (text != null && text.length() > MAX_NOTE) {
            throw new BadRequestException("Keep the note under " + MAX_NOTE + " characters");
        }
        // Only a pupil of this class - the endpoint's permission is for this class.
        enrollmentRepo.findByClassAndStudentAndSchoolId(classId, studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("That pupil isn't in this class"));

        String name = userRepo.findById(userId).map(u -> u.getName()).orElse(null);
        var followUp = StudentFollowUp.record(schoolId, studentId, classId, parsed, text, resolved, userId, name);
        repo.save(followUp);
        return toDTO(followUp);
    }

    private static FollowUpDTO toDTO(StudentFollowUp f) {
        return new FollowUpDTO(f.getId(), f.getStudentId(), f.getAction().name(), f.getNote(), f.isResolved(),
                f.getRecordedByName(), f.getCreatedAt());
    }
}
