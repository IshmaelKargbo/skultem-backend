package com.moriba.skultem.application.dto;

import java.util.List;

import com.moriba.skultem.domain.model.Term;

public record ActiveCycleDTO(String id, String year, String clazz, List<Term> terms) {
}
