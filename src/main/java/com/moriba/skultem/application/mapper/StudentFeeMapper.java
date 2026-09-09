package com.moriba.skultem.application.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.dto.StudentFeeDTO;
import com.moriba.skultem.domain.model.StudentFee;

public class StudentFeeMapper {

    public static StudentFeeDTO toDTO(StudentFee param, BigDecimal amountPaid) {
        if (param == null)
            return null;

        FeeStructureDTO fee = FeeStructureMapper.toDTO(param.getFee());
        StudentDTO student = StudentMapper.toDTO(param.getStudent(), param.getEnrollment());

        String clazz = param.getEnrollment().getClazz().getName();
        String studentName = String.join(" ", student.givenNames(), student.familyName());

        // Net of any discount linked to this student's fee - without this a discounted fee
        // never shows as fully paid/settled even once the (lower) amount actually owed has
        // been paid in full.
        BigDecimal discount = param.getDiscount() != null ? param.getDiscount().computeSavings() : BigDecimal.ZERO;
        BigDecimal netPayable = fee.amount().subtract(discount);
        BigDecimal balance = netPayable.subtract(amountPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        String status = resolveStatus(amountPaid, netPayable, fee.dueDate());

        return new StudentFeeDTO(
                student.id(),
                studentName,
                clazz,
                fee.term().name(),
                fee.category().name(),
                fee.amount(),
                amountPaid,
                balance,
                fee.dueDate(),
                status,
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    private static String resolveStatus(BigDecimal amountPaid, BigDecimal netPayable, LocalDate dueDate) {
        if (amountPaid.compareTo(netPayable) >= 0) {
            return "Paid";
        }

        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return "Overdue";
        }

        if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            return "Partial";
        }

        return "Pending";
    }
}