package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.SchoolFeeRowDTO;
import com.moriba.skultem.domain.repository.PaymentRepository;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.service.FeeCollectionCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * The shared building block behind every Fees Reporting use case (dashboard, term summary,
 * student balances, outstanding fees): every school-fee charge for one academic year (optionally
 * one term), net of any discount, matched against what's actually been paid. The platform fee
 * (FeeStructure#system) is never included - see StudentFeeJpaRepository#findSchoolFeeRows and
 * PaymentJpaRepository#sumSchoolPaymentsGroupedByStudentAndFee, the only two queries this touches.
 *
 * Two queries total regardless of how many students/fees the school has (a fetch-joined list of
 * StudentFee rows, and one grouped sum of payments) - not one query per row - so a school-wide
 * dashboard stays fast.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LoadSchoolFeeRowsUseCase {

    private final StudentFeeRepository studentFeeRepo;
    private final PaymentRepository paymentRepo;

    public List<SchoolFeeRowDTO> execute(String schoolId, String academicYearId, String termId) {
        var fees = studentFeeRepo.findSchoolFeeRows(schoolId, academicYearId, termId);

        Map<String, BigDecimal> paidByStudentAndFee = new HashMap<>();
        for (Object[] row : paymentRepo.sumSchoolPaymentsGroupedByStudentAndFee(schoolId, academicYearId, termId)) {
            paidByStudentAndFee.put(row[0] + "|" + row[1], (BigDecimal) row[2]);
        }

        List<SchoolFeeRowDTO> rows = new ArrayList<>();
        for (var studentFee : fees) {
            var fee = studentFee.getFee();
            var student = studentFee.getStudent();
            var enrollment = studentFee.getEnrollment();
            var clazz = enrollment.getClazz();
            var section = enrollment.getSection();
            var stream = enrollment.getStream();

            BigDecimal discount = studentFee.getDiscount() != null
                    ? studentFee.getDiscount().computeSavings()
                    : BigDecimal.ZERO;
            BigDecimal netPayable = fee.getAmount().subtract(discount);
            BigDecimal paid = paidByStudentAndFee.getOrDefault(student.getId() + "|" + fee.getId(), BigDecimal.ZERO);
            BigDecimal balance = FeeCollectionCalculator.balance(netPayable, paid);
            var status = FeeCollectionCalculator.resolveStatus(netPayable, paid);

            String classSessionKey = clazz.getId() + "|" + (section != null ? section.getId() : "")
                    + "|" + (stream != null ? stream.getId() : "");
            String className = stream != null ? clazz.getName() + " (" + stream.getName() + ")" : clazz.getName();

            rows.add(new SchoolFeeRowDTO(
                    student.getId(),
                    student.getGivenNames() + " " + student.getFamilyName(),
                    student.getAdmissionNumber(),
                    classSessionKey,
                    className,
                    fee.getCategory().getId(),
                    fee.getCategory().getName(),
                    fee.getTerm().getId(),
                    fee.getTerm().getName(),
                    netPayable,
                    paid,
                    balance,
                    status));
        }

        return rows;
    }
}
