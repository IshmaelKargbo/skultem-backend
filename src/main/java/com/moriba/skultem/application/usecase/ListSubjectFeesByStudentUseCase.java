package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.application.mapper.StudentFeeMapper;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListSubjectFeesByStudentUseCase {

    private final StudentFeeRepository repo;
    private final PaymentRepository paymentRepo;
    private final AcademicYearRepository academicYearRepo;

    /**
     * A student's fees for one academic year - the one asked for, or the school's active year when none
     * is given - not every fee they've ever been charged. (The header's year switcher already sends the
     * selected year with every request; this is what finally honours it.) A school with no active year
     * has no fees to show, which is an empty list rather than an error.
     */
    public Page<StudentFeeDTO> execute(String school, String studentId, String academicYearId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page, size);
        }

        String yearId;
        if (academicYearId != null && !academicYearId.isBlank()) {
            yearId = academicYearRepo.findByIdAndSchoolId(academicYearId, school)
                    .orElseThrow(() -> new NotFoundException("Academic year not found"))
                    .getId();
        } else {
            var active = academicYearRepo.findActiveBySchool(school);
            if (active.isEmpty()) {
                return Page.empty(pageable);
            }
            yearId = active.get().getId();
        }

        return repo.findAllBySchoolAndStudentAndAcademicYear(school, studentId, yearId, pageable).map(e -> {
            // Was hardcoded to ZERO here, so every fee looked fully unpaid ("Pending", full
            // balance owed) on the parent's Fee Schedule regardless of what had actually been
            // paid - this is the real, per-fee sum of that student's payments against it.
            BigDecimal amountPaid = Optional
                    .ofNullable(paymentRepo.sumPaymentsByStudentAndFee(e.getStudent().getId(), e.getFee().getId()))
                    .orElse(BigDecimal.ZERO);

            return StudentFeeMapper.toDTO(e, amountPaid);
        });
    }
}
