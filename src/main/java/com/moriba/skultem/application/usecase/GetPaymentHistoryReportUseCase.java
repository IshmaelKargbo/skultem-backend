package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeePaymentRowDTO;
import com.moriba.skultem.domain.model.Payment.PaymentMethod;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Every school-fee payment recorded during a period, narrowed by whichever filters are given -
 * backs Payment History and (scoped to one day) the Daily Collection transaction table. Reuses the
 * school-fee-only payment search (platform fee excluded) - see PaymentJpaRepository#searchSchoolPayments.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GetPaymentHistoryReportUseCase {

    private final PaymentRepository paymentRepo;
    private final ClassSessionRepository classSessionRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    public Page<FeePaymentRowDTO> execute(String schoolId, Instant from, Instant to, String academicYearId,
            String termId, String classSessionId, String studentId, PaymentMethod method, String recordedByUserId,
            int page, int size) {

        String classId = null;
        String sectionId = null;
        String streamId = null;
        if (classSessionId != null && !classSessionId.isBlank()) {
            var session = classSessionRepo.findByIdAndSchoolId(classSessionId, schoolId).orElse(null);
            if (session == null) {
                return Page.empty();
            }
            classId = session.getClazz().getId();
            sectionId = session.getSection().getId();
            streamId = session.getStream() != null ? session.getStream().getId() : null;
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "paidAt");
        Pageable pageable = size > 0 ? PageRequest.of(page, size, sort) : Pageable.unpaged(sort);

        var payments = paymentRepo.searchSchoolPayments(schoolId, from, to, academicYearId, termId, classId,
                sectionId, streamId, studentId, method, recordedByUserId, pageable);

        Map<String, String> recorderCache = new HashMap<>();
        Map<String, String> classNameCache = new HashMap<>();

        return payments.map(payment -> {
            var student = payment.getStudent();
            var fee = payment.getFee();
            String recordedBy = resolveRecorder(payment.getRecordedByUserId(), recorderCache);
            String className = resolveClassName(schoolId, student.getId(), fee.getAcademicYear().getId(),
                    classNameCache);

            return new FeePaymentRowDTO(payment.getId(), payment.getPaidAt(), payment.getReferenceNo(),
                    student.getId(), student.getGivenNames() + " " + student.getFamilyName(), className,
                    fee.getCategory().getName(), payment.getAmount(), payment.getMethod().name(), recordedBy);
        });
    }

    private String resolveRecorder(String userId, Map<String, String> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, id -> userRepo.findById(id).map(u -> u.getName()).orElse(null));
    }

    // A payment doesn't carry the class the student was in when it was paid - resolved from their
    // enrollment for the fee's own academic year instead, same as everywhere else "class" is shown
    // alongside a fee. Cached per (student, year) since a page of payments repeats both often.
    private String resolveClassName(String schoolId, String studentId, String academicYearId,
            Map<String, String> cache) {
        String key = studentId + "|" + academicYearId;
        return cache.computeIfAbsent(key, k -> enrollmentRepo
                .findByStudentAndAcademicYearAndSchoolId(studentId, academicYearId, schoolId)
                .map(e -> e.getStream() != null ? e.getClazz().getName() + " (" + e.getStream().getName() + ")"
                        : e.getClazz().getName())
                .orElse("N/A"));
    }
}
