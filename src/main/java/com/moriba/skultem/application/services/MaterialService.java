package com.moriba.skultem.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.MaterialCategoryDTO;
import com.moriba.skultem.application.dto.MaterialDTO;
import com.moriba.skultem.application.dto.MaterialTrasactionDTO;
import com.moriba.skultem.application.dto.SupplyDTO;
import com.moriba.skultem.application.mapper.MaterialCategoryMapper;
import com.moriba.skultem.application.mapper.MaterialMapper;
import com.moriba.skultem.application.mapper.MaterialTransactionMapper;
import com.moriba.skultem.application.mapper.SupplyMapper;
import com.moriba.skultem.application.usecase.CreateMaterialCategoryUseCase;
import com.moriba.skultem.application.usecase.CreateMaterialUseCase;
import com.moriba.skultem.application.usecase.CreateSupplyUseCase;
import com.moriba.skultem.application.usecase.ReStockMaterialUseCase;
import com.moriba.skultem.application.usecase.SupplyMaterialUseCase;
import com.moriba.skultem.domain.model.Material.Unit;
import com.moriba.skultem.domain.repository.MaterialCategoryRepository;
import com.moriba.skultem.domain.repository.MaterialRepository;
import com.moriba.skultem.domain.repository.MaterialTransactionRepository;
import com.moriba.skultem.domain.repository.SupplyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final CreateMaterialCategoryUseCase createMaterialCategoryUseCase;
    private final CreateMaterialUseCase createMaterialUseCase;
    private final MaterialCategoryRepository categoryRepo;
    private final MaterialTransactionRepository materialTransactionRepo;
    private final MaterialRepository materialRepo;
    private final SupplyRepository supplyRepo;
    private final CreateSupplyUseCase createSupplyUseCase;
    private final SupplyMaterialUseCase supplyMaterialUseCase;
    private final ReStockMaterialUseCase reStockMaterialUseCase;

    public MaterialCategoryDTO createMaterialCategory(String schoolId, String name, String description) {
        return createMaterialCategoryUseCase.execute(schoolId, name, description);
    }

    public MaterialDTO createMaterial(String schoolId, String name, Unit unit, int qty, String categoryId) {
        return createMaterialUseCase.execute(schoolId, name, unit, qty, categoryId);
    }

    public MaterialDTO restockMaterial(String schoolId, String id, int qty, String note) {
        return reStockMaterialUseCase.execute(schoolId, id, qty, note);
    }

    public SupplyDTO supplyMaterial(String schoolId, String id, int qty, String note) {
        return supplyMaterialUseCase.execute(schoolId, id, qty, note);
    }

    public SupplyDTO createSupply(String schoolId, String studentId, String materialId, int qty) {
        return createSupplyUseCase.execute(schoolId, studentId, materialId, qty);
    }

    public Page<MaterialCategoryDTO> listCategory(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return categoryRepo.findBySchool(school, pageable).map(MaterialCategoryMapper::toDTO);
    }

    public Page<MaterialDTO> listMaterial(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return materialRepo.findBySchool(school, pageable).map(MaterialMapper::toDTO);
    }

    public Page<MaterialTrasactionDTO> listMaterialTransaction(String school, String materialId, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return materialTransactionRepo.findByMaterialIdAndSchool(materialId, school, pageable)
                .map(MaterialTransactionMapper::toDTO);
    }

    public Page<SupplyDTO> listSupply(String school, int page, int size) {
        Pageable pageable = Pageable.unpaged();

        if (size > 0) {
            pageable = PageRequest.of(page - 1, size);
        }

        return supplyRepo.findBySchool(school, pageable).map(SupplyMapper::toDTO);
    }
}
