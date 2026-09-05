package com.moriba.skultem.application.mapper;

import java.util.List;

import com.moriba.skultem.domain.vo.PayComponent;
import com.moriba.skultem.domain.vo.PayComponentType;
import com.moriba.skultem.infrastructure.rest.dto.PayComponentItemDTO;

// Shared by CreateSalaryTemplateUseCase / UpdateSalaryTemplateUseCase / SetSalaryStructureUseCase -
// each one always replaces its whole allowances/deductions list wholesale on save, so there's
// nothing to preserve from the previous list; converting the client's request items straight into
// fresh PayComponents is all any of them need.
public class PayComponentItemMapper {
    public static List<PayComponent> toDomain(List<PayComponentItemDTO> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(item -> new PayComponent(item.name().trim(), PayComponentType.valueOf(item.type()), item.value()))
                .toList();
    }
}
