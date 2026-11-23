package com.moriba.skultem.domain.repository;

import java.util.Map;

import com.moriba.skultem.domain.repository.ParentPurgeRepository.AccountOutcome;

// Removes a staff member added by mistake (see DeleteTeacherPermanentlyUseCase).
public interface TeacherPurgeRepository {

    /** Work and history that still point at this staff member, by a label a school would recognise. Only kinds in use. */
    Map<String, Long> history(String teacherId);

    /** Deletes the staff record and their access to this school; the login account only if nothing else uses it. */
    AccountOutcome purge(String schoolId, String teacherId);
}
