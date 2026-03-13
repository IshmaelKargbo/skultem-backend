package com.moriba.skultem.infrastructure.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class ReportExportUtil {

    private static final int PDF_MAX_LINE = 180;

    public static byte[] toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCsv(headers)).append("\n");
        for (List<String> row : rows) {
            sb.append(joinCsv(row)).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] toPdf(String title, List<String> headers, List<List<String>> rows) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margin = 40;
            float yStart = page.getMediaBox().getHeight() - margin;
            float leading = 14f;

            PDPageContentStream content = new PDPageContentStream(document, page);
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            float yPosition = yStart;
            content.beginText();
            content.newLineAtOffset(margin, yPosition);

            List<String> lines = buildLines(title, headers, rows);
            for (String line : lines) {
                if (yPosition <= margin) {
                    content.endText();
                    content.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    content = new PDPageContentStream(document, page);
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    yPosition = yStart;
                    content.beginText();
                    content.newLineAtOffset(margin, yPosition);
                }

                content.showText(line);
                content.newLineAtOffset(0, -leading);
                yPosition -= leading;
            }

            content.endText();
            content.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }

    private static List<String> buildLines(String title, List<String> headers, List<List<String>> rows) {
        List<String> headerLine = List.of(truncate(title));
        List<String> tableHeader = List.of(truncate(String.join(" | ", headers)));
        List<String> tableRows = rows.stream()
                .map(row -> truncate(row.stream().map(ReportExportUtil::safe).collect(Collectors.joining(" | "))))
                .toList();

        return new java.util.ArrayList<String>() {{
            addAll(headerLine);
            add("");
            addAll(tableHeader);
            add("");
            addAll(tableRows);
        }};
    }

    private static String joinCsv(List<String> values) {
        return values.stream().map(ReportExportUtil::escapeCsv).collect(Collectors.joining(","));
    }

    private static String escapeCsv(String value) {
        String safe = safe(value);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = safe.replace("\"", "\"\"");
            return "\"" + safe + "\"";
        }
        return safe;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= PDF_MAX_LINE) {
            return value;
        }
        return value.substring(0, PDF_MAX_LINE - 3) + "...";
    }
}
