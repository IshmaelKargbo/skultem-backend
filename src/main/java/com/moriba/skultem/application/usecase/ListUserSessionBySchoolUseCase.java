package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.UserSessionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListUserSessionBySchoolUseCase {
    private final UserSessionRepository repo;

    public Page<UserSession> execute(String schoolId, int page, int size) {
        Pageable pageable = Pageable.unpaged();
        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }
        return repo.findAllBySchoolId(schoolId, pageable);
    }
}
