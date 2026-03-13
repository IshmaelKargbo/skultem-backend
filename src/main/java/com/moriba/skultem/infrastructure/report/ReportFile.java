package com.moriba.skultem.infrastructure.report;

public record ReportFile(String filename, String contentType, byte[] data) {
}
