package com.moriba.skultem.application.mapper;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.domain.model.MaterialSale;

public class MaterialSaleMapper {
    public static MaterialSaleDTO toDTO(MaterialSale param) {
        if (param == null) return null;

        var student = param.getStudent() != null ? StudentMapper.toDTO(param.getStudent(), null) : null;
        var material = MaterialMapper.toDTO(param.getMaterial());

        return new MaterialSaleDTO(
                param.getId(),
                student,
                param.getCustomerName(),
                material,
                param.getQuantity(),
                param.getUnitPrice(),
                param.getTotalAmount(),
                param.getAmountPaid(),
                param.getBalance(),
                param.getPaymentStatus(),
                param.getPaymentMethod(),
                param.getNote(),
                param.getStatus(),
                param.getFulfilledAt(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }
}
