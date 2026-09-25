package com.moriba.skultem.domain.repository;

// Hard-deletes a parent (guardian) record - for one added by mistake.
public interface ParentPurgeRepository {

    // Students whose primary guardian this parent is - a student can't exist without one, so any
    // here means the parent can't be deleted until those students are.
    long primaryStudentCount(String schoolId, String parentId);

    Purged purge(String schoolId, String parentId);

    // accountRemoved: the login account went too (it belonged to nobody else); otherwise it was only
    // detached from this school, or - when other records still point at it - disabled.
    record Purged(int otherStudentLinksRemoved, AccountOutcome account) {
    }

    enum AccountOutcome {
        DELETED, DISABLED, KEPT
    }
}
