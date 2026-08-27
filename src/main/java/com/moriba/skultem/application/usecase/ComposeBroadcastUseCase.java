package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BroadcastDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.BroadcastMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Broadcast;
import com.moriba.skultem.domain.repository.BroadcastRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.Audience;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ComposeBroadcastUseCase {
    private final BroadcastRepository repo;
    private final UserRepository userRepo;
    private final GetAudienceSizeUseCase getAudienceSizeUseCase;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "BROADCAST_COMPOSED")
    public BroadcastDTO execute(String schoolId, String userId, String title, String message, Audience audience,
            List<Broadcast.Channel> channels, Broadcast.SendOption sendOption, Instant scheduledAt) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("Select at least one channel");
        }

        var user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var id = UUID.randomUUID().toString();
        var sentByName = user.getGivenNames() + " " + user.getFamilyName();
        int recipientsCount = getAudienceSizeUseCase.execute(schoolId, audience);

        var broadcast = Broadcast.compose(id, schoolId, title, message, audience, channels, sendOption, scheduledAt,
                recipientsCount, userId, sentByName);
        repo.save(broadcast);

        logActivityUseCase.log(schoolId, ActivityType.SCHOOL, "Broadcast composed", broadcast.getTitle(), null,
                broadcast.getId());

        return BroadcastMapper.toDTO(broadcast);
    }
}
