package com.moriba.skultem.application.mapper;

import java.math.BigDecimal;

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
        BigDecimal balance = fee.amount().subtract(amountPaid);

        String status = resolveStatus(amountPaid, fee.amount());

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

    private static String resolveStatus(BigDecimal amountPaid, BigDecimal feeAmount) {
        if (amountPaid.compareTo(feeAmount) >= 0) {
            return "Paid";
        } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            return "Partial";
        } else {
            return "Pending";
        }
    }
}