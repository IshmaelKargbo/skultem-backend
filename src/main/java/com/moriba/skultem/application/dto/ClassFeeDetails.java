package com.moriba.skultem.application.dto;

import java.util.List;
import java.util.Map;

public record ClassFeeDetails(String sessionId, String termId, Summery summery, List<Record> records, Map<String, Object> meta) {
    public record Summery(int paid, int pending, int partial) {
    }

    public record Record(String id, String name, String admissionNo, String status) {
    }
}
