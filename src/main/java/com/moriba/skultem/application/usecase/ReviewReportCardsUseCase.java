package com.moriba.skultem.application.usecase;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.application.services.NotificationService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Priority;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The report card cycle after generating: the admin generates a class's cards, its class teacher is told, checks
// them, adds comments, and approves them (then parents can see them) or sends them back to the admin with a note
// saying what's wrong. The admin fixes the scores and generates again, which puts them up for review again.
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewReportCardsUseCase {

    private final ReportCardRepository repo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationService notificationService;
    private final PermissionService permissionService;
    private final com.moriba.skultem.domain.repository.TeacherRepository teacherRepo;

    public record Result(int count, String reviewStatus) {
    }

    // ids narrows it to some students' cards; empty = every card of the class for the term still under review.
    @AuditLogAnnotation(action = "REPORT_CARDS_APPROVED")
    public Result approve(String schoolId, String classId, String termId, Collection<String> ids) {
        var cards = pick(schoolId, classId, termId, ids);
        if (cards.isEmpty()) {
            throw new RuleException("There are no report cards waiting for approval here");
        }
        String by = currentName();
        // The class teacher signs what they approve; an admin approving a class without one leaves the line blank.
        var user = PermissionService.getCurrentUser();
        var teacher = teacherRepo.findByUserIdAndSchoolId(user.userId(), schoolId).orElse(null);
        boolean isClassTeacher = teacher != null && permissionService.classMasterUserIds(schoolId, classId)
                .contains(user.userId());
        String signName = isClassTeacher ? by : null;
        String signature = isClassTeacher ? teacher.getSignature() : null;
        cards.forEach(c -> {
            c.approve(by, signName, signature);
            repo.save(c);
        });
        notifyGenerators(schoolId, cards, "Report cards approved",
                by + " approved " + cards.size() + " " + cardWord(cards.size()) + " for " + label(cards)
                        + ". Parents can now see them.");
        return new Result(cards.size(), ReportCard.ReviewStatus.APPROVED.name());
    }

    @AuditLogAnnotation(action = "REPORT_CARDS_RETURNED")
    public Result sendBack(String schoolId, String classId, String termId, Collection<String> ids, String note) {
        if (note == null || note.isBlank()) {
            throw new RuleException("Say what needs fixing, so the admin knows what to change");
        }
        var cards = pick(schoolId, classId, termId, ids);
        if (cards.isEmpty()) {
            throw new RuleException("There are no report cards waiting for review here");
        }
        String by = currentName();
        cards.forEach(c -> {
            c.returnForChanges(note.trim(), by);
            repo.save(c);
        });
        notifyGenerators(schoolId, cards, "Report cards sent back",
                by + " sent back " + cards.size() + " " + cardWord(cards.size()) + " for " + label(cards) + ": "
                        + note.trim());
        return new Result(cards.size(), ReportCard.ReviewStatus.RETURNED.name());
    }

    /** After generating: tell the class's class teachers there are cards to check. */
    public void notifyReviewers(String schoolId, String classId, String className, String termName, int count) {
        if (count == 0) {
            return;
        }
        var meta = Map.of("class_id", classId, "type", "report_card_review");
        for (var userId : permissionService.classMasterUserIds(schoolId, classId)) {
            send(schoolId, userId, "Report cards to review",
                    count + " " + cardWord(count) + " for " + className + " (" + termName
                            + ") are ready for you to check. Add your comments, then approve them.",
                    meta);
        }
    }

    // Cards of the class/term not yet approved - or just the chosen ones.
    private List<ReportCard> pick(String schoolId, String classId, String termId, Collection<String> ids) {
        var wanted = ids == null ? java.util.Set.<String>of() : new LinkedHashSet<>(ids);
        return repo.findAllBySchoolIdAndClassIdAndTermId(schoolId, classId, termId).stream()
                .filter(c -> !c.isApproved())
                .filter(c -> wanted.isEmpty() || wanted.contains(c.getId()))
                .toList();
    }

    private void notifyGenerators(String schoolId, List<ReportCard> cards, String title, String message) {
        var meta = Map.of("class_id", cards.get(0).getClassId(), "type", "report_card_review");
        cards.stream().map(ReportCard::getGeneratedBy).filter(java.util.Objects::nonNull).distinct()
                .forEach(userId -> send(schoolId, userId, title, message, meta));
    }

    private void send(String schoolId, String userId, String title, String message, Map<String, String> meta) {
        var owner = userRepo.findById(userId).orElse(null);
        if (owner == null) {
            return;
        }
        var n = Notification.create(UUID.randomUUID().toString(), schoolId, owner, Notification.Type.ASSESSMENT,
                title, message, meta, Priority.NORMAL);
        notificationRepo.save(n);
        long count = notificationRepo.countAllOpenByOwnerAndSchoolId(userId, schoolId);
        notificationService.sendToUser(userId, count, NotificationMapper.toDTO(n));
    }

    private String currentName() {
        var user = PermissionService.getCurrentUser();
        return userRepo.findById(user.userId()).map(u -> u.getName()).orElse("The class teacher");
    }

    private static String label(List<ReportCard> cards) {
        var c = cards.get(0);
        return c.getClassName() + " (" + c.getTermName() + ")";
    }

    private static String cardWord(int n) {
        return n == 1 ? "report card" : "report cards";
    }
}
