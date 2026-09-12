package com.moriba.skultem.infrastructure.persistence.mapper;

import com.moriba.skultem.domain.model.MaterialSale;
import com.moriba.skultem.infrastructure.persistence.entity.MaterialSaleEntity;

public class MaterialSaleMapper {
    public static MaterialSale toDomain(MaterialSaleEntity param) {
        if (param == null) return null;

        var student = param.getStudent() != null ? StudentMapper.toDomain(param.getStudent()) : null;
        var material = MaterialMapper.toDomain(param.getMaterial());

        return new MaterialSale(
                param.getId(),
                param.getSchoolId(),
                student,
                param.getCustomerName(),
                material,
                param.getQuantity(),
                param.getUnitPrice(),
                param.getTotalAmount(),
                param.getAmountPaid(),
                param.getPaymentMethod(),
                param.getNote(),
                param.getStatus(),
                param.getFulfilledAt(),
                param.getSupplyId(),
                param.getCreatedAt(),
                param.getUpdatedAt());
    }

    public static MaterialSaleEntity toEntity(MaterialSale param) {
        if (param == null) return null;

        var student = param.getStudent() != null ? StudentMapper.toEntity(param.getStudent()) : null;
        var material = MaterialMapper.toEntity(param.getMaterial());

        return MaterialSaleEntity.builder()
                .id(param.getId())
                .schoolId(param.getSchoolId())
                .student(student)
                .customerName(param.getCustomerName())
                .material(material)
                .quantity(param.getQuantity())
                .unitPrice(param.getUnitPrice())
                .totalAmount(param.getTotalAmount())
                .amountPaid(param.getAmountPaid())
                .paymentMethod(param.getPaymentMethod())
                .note(param.getNote())
                .status(param.getStatus())
                .fulfilledAt(param.getFulfilledAt())
                .supplyId(param.getSupplyId())
                .createdAt(param.getCreatedAt())
                .updatedAt(param.getUpdatedAt())
                .build();
    }
}
