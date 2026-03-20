package com.moriba.skultem.application.usecase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NotificationDTO;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.domain.repository.NotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListNotificationByParentUseCase {

    private final NotificationRepository repo;

    public Page<NotificationDTO> execute(String schoolId, String parentId, int page, int size) {

        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(
                    page - 1,
                    size,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return repo
                .findAllByOwnerAndSchoolId(parentId, schoolId, pageable)
                .map(NotificationMapper::toDTO);
    }
}