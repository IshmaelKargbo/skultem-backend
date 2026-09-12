package com.moriba.skultem.application.services;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialSaleDTO;
import com.moriba.skultem.application.dto.MaterialSaleSummaryDTO;
import com.moriba.skultem.application.mapper.MaterialSaleMapper;
import com.moriba.skultem.application.usecase.CancelMaterialSaleUseCase;
import com.moriba.skultem.application.usecase.CreateMaterialSaleUseCase;
import com.moriba.skultem.application.usecase.FulfillMaterialSaleUseCase;
import com.moriba.skultem.application.usecase.GetMaterialSaleSummaryUseCase;
import com.moriba.skultem.application.usecase.RecordMaterialSalePaymentUseCase;
import com.moriba.skultem.domain.model.MaterialSale.PaymentMethod;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialSaleService {

    private final MaterialSaleRepository repo;
    private final CreateMaterialSaleUseCase createMaterialSaleUseCase;
    private final FulfillMaterialSaleUseCase fulfillMaterialSaleUseCase;
    private final RecordMaterialSalePaymentUseCase recordMaterialSalePaymentUseCase;
    private final CancelMaterialSaleUseCase cancelMaterialSaleUseCase;
    private final GetMaterialSaleSummaryUseCase getMaterialSaleSummaryUseCase;

    public MaterialSaleDTO create(String schoolId, String materialId, String studentId, String customerName,
            int quantity, BigDecimal unitPrice, BigDecimal amountPaid, PaymentMethod paymentMethod, String note,
            boolean collectNow) {
        return createMaterialSaleUseCase.execute(schoolId, materialId, studentId, customerName, quantity, unitPrice,
                amountPaid, paymentMethod, note, collectNow);
    }

    public MaterialSaleDTO fulfill(String schoolId, String id, String note) {
        return fulfillMaterialSaleUseCase.execute(schoolId, id, note);
    }

    public MaterialSaleDTO recordPayment(String schoolId, String id, BigDecimal amount, PaymentMethod method) {
        return recordMaterialSalePaymentUseCase.execute(schoolId, id, amount, method);
    }

    public MaterialSaleDTO cancel(String schoolId, String id) {
        return cancelMaterialSaleUseCase.execute(schoolId, id);
    }

    public MaterialSaleSummaryDTO summary(String schoolId) {
        return getMaterialSaleSummaryUseCase.execute(schoolId);
    }

    public MaterialSaleDTO findOne(String schoolId, String id) {
        return repo.findByIdAndSchool(id, schoolId).map(MaterialSaleMapper::toDTO).orElse(null);
    }

    public Page<MaterialSaleDTO> list(String school, int page, int size, String search, Status status,
            boolean paidPending) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return repo.filter(school, search, status, paidPending, pageable).map(MaterialSaleMapper::toDTO);
    }
}
