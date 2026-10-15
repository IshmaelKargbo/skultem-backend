package com.moriba.skultem.application.usecase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.FeeDiscountDTO;
import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.utils.MoneyUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListFeeDiscountBySchoolUseCase {
    private final FeeDiscountRepository repo;

    // Discounts are stored one row per fee but shown one per discount, so the rows are grouped here and
    // the page is cut from the groups (not the rows).
    public Page<FeeDiscountDTO> execute(String schoolId, int page, int size) {
        var groups = new LinkedHashMap<String, List<FeeDiscount>>();
        for (var row : repo.findAllBySchoolOrderByCreatedAtDesc(schoolId)) {
            groups.computeIfAbsent(row.getGroupId(), k -> new ArrayList<>()).add(row);
        }

        var all = groups.values().stream().map(this::toDTO).toList();

        if (size <= 0) {
            return new PageImpl<>(all);
        }

        Pageable pageable = PageRequest.of(page, size);
        int from = (int) Math.min(pageable.getOffset(), all.size());
        int to = Math.min(from + size, all.size());
        return new PageImpl<>(all.subList(from, to), pageable, all.size());
    }

    private FeeDiscountDTO toDTO(List<FeeDiscount> rows) {
        var first = rows.get(0);
        Student student = first.getEnrollment().getStudent();
        String name = String.join(" ", student.getGivenNames(), student.getFamilyName());

        BigDecimal total = rows.stream().map(FeeDiscount::computeSavings).reduce(BigDecimal.ZERO, BigDecimal::add);
        var fees = rows.stream()
                .map(r -> new FeeDiscountDTO.Fee(r.getFee().getCategory().getName(), r.getFee().getTerm().getName(),
                        r.value(), MoneyUtil.format(r.computeSavings())))
                .toList();

        return new FeeDiscountDTO(
                first.getGroupId(),
                first.getName(),
                first.getReason(),
                name,
                first.getClazz(),
                first.getKind().name(),
                rows.stream().map(FeeDiscount::value).distinct().count() > 1 ? "Varies" : first.value(),
                first.appliedDate(),
                first.getExpiryDate(),
                MoneyUtil.format(total),
                fees);
    }
}
