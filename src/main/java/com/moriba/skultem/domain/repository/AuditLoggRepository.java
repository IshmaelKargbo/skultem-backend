package com.moriba.skultem.domain.repository;

import java.util.List;

import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.model.User;

public interface AuditLoggRepository {
    void save(AuditLog domain);

    List<AuditLog> findAllBySchool(User user, boolean active);
}
