package com.moriba.skultem.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.MaterialSale;
import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.domain.repository.MaterialSaleRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.MaterialSaleJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.MaterialSaleMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MaterialSaleAdapter implements MaterialSaleRepository {

    private final MaterialSaleJpaRepository repo;

    @Override
    public void save(MaterialSale domain) {
        var entity = MaterialSaleMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<MaterialSale> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(MaterialSaleMapper::toDomain);
    }

    @Override
    public Page<MaterialSale> findByStudentAndSchool(String studentId, String schoolId, Pageable pageable) {
        return repo.findAllByStudentIdAndSchoolIdOrderByCreatedAtDesc(studentId, schoolId, pageable)
                .map(MaterialSaleMapper::toDomain);
    }

    @Override
    public Page<MaterialSale> filter(String schoolId, String query, Status status, boolean paidPending,
            Pageable pageable) {
        // Wildcards are added here, in Java, rather than via concat('%', :query, '%') in JPQL -
        // see the comment on MaterialSaleJpaRepository.filter for why that broke on a null query.
        String pattern = query != null && !query.isBlank() ? "%" + query.trim().toLowerCase() + "%" : null;
        return repo.filter(schoolId, pattern, status, paidPending, Status.PENDING_SUPPLY, pageable)
                .map(MaterialSaleMapper::toDomain);
    }

    @Override
    public long countBySchool(String schoolId) {
        return repo.countBySchoolId(schoolId);
    }

    @Override
    public long countBySchoolAndStatus(String schoolId, Status status) {
        return repo.countBySchoolIdAndStatus(schoolId, status);
    }

    @Override
    public long countAwaitingCollectionWithPayment(String schoolId) {
        return repo.countBySchoolIdAndStatusAndAmountPaidGreaterThan(schoolId, Status.PENDING_SUPPLY,
                BigDecimal.ZERO);
    }

    @Override
    public BigDecimal sumAmountPaidBySchool(String schoolId) {
        return repo.sumAmountPaidBySchoolId(schoolId, Status.CANCELLED);
    }

    @Override
    public BigDecimal sumOutstandingBalanceBySchool(String schoolId) {
        return repo.sumOutstandingBalanceBySchoolId(schoolId, Status.CANCELLED);
    }
}
