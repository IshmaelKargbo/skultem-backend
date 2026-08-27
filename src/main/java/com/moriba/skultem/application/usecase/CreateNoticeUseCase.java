package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.NoticeDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.NoticeMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Notice;
import com.moriba.skultem.domain.repository.NoticeRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Audience;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateNoticeUseCase {
    private final NoticeRepository repo;
    private final UserRepository userRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "NOTICE_CREATED")
    public NoticeDTO execute(String schoolId, String userId, String title, String content, Notice.Category category,
            Audience audience, Instant expiresAt) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var id = UUID.randomUUID().toString();
        var postedByName = user.getGivenNames() + " " + user.getFamilyName();
        var notice = Notice.create(id, schoolId, title, content, category, audience, expiresAt, userId,
                postedByName);
        repo.save(notice);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "Notice posted", notice.getTitle(), null,
                notice.getId());

        return NoticeMapper.toDTO(notice);
    }
}
