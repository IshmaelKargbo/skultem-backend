package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.FeeStructureDTO;
import com.moriba.skultem.application.dto.PaymentDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.domain.model.Payment;

public class PaymentMapper {
    public static PaymentDTO toDTO(Payment param) {
        StudentDTO student = StudentMapper.toDTO(param.getStudent());
        FeeStructureDTO fee = FeeStructureMapper.toDTO(param.getFee());

        return new PaymentDTO(param.getId(), student, fee, param.getAmount(), param.getPaidAt(), param.getMethod(),
                param.getReferenceNo(), param.getNote(), param.getCreatedAt(), param.getUpdatedAt());
    }
}
