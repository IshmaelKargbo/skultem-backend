package com.moriba.skultem.application.usecase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ReportCardDTO;
import com.moriba.skultem.application.dto.SchoolDTO;
import com.moriba.skultem.application.error.DocumentServiceException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.services.DocumentDesignService;
import com.moriba.skultem.application.services.ReportCardDocumentData;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.report.ReportFile;

/**
 * Report cards as PDFs made by M-Compose, from the design the school picked (DocumentDesignService), else the
 * server's m-compose.templates.report-card. With neither, or without M-Compose, isEnabled() is false and the app
 * keeps drawing the PDF in the browser as before.
 */
@Service
public class RenderReportCardPdfUseCase {
    private static final Logger log = LoggerFactory.getLogger(RenderReportCardPdfUseCase.class);
    private static final String PDF = "application/pdf";
    // The engine's own default limit on one batch.
    private static final int MAX_CARDS = 500;

    private final GetReportCardUseCase getReportCardUseCase;
    private final ReportCardRepository repo;
    private final R2StorageService storage;
    private final MComposeClient compose;
    private final DocumentDesignService designs;
    private final com.moriba.skultem.application.services.DocumentFieldLinkService links;

    public RenderReportCardPdfUseCase(GetReportCardUseCase getReportCardUseCase, ReportCardRepository repo,
            R2StorageService storage, MComposeClient compose, DocumentDesignService designs,
            com.moriba.skultem.application.services.DocumentFieldLinkService links) {
        this.getReportCardUseCase = getReportCardUseCase;
        this.repo = repo;
        this.storage = storage;
        this.compose = compose;
        this.designs = designs;
        this.links = links;
    }

    /** On for this school: M-Compose is set up and the school (or the server) has a report card design. */
    public boolean isEnabled(String schoolId) {
        return compose.isEnabled() && !designs.templateFor(schoolId, DocumentKind.REPORT_CARD).isEmpty();
    }

    /**
     * The school's report card design as it prints: filled with the school's latest generated card when there is
     * one, else with placeholders. template: a design to try before choosing it; blank = the school's current one.
     */
    public ReportFile preview(String schoolId, String template, SchoolDTO school) {
        String ref = template == null || template.isBlank() ? requireTemplate(schoolId) : template.trim();
        if (!compose.isEnabled()) {
            requireTemplate(schoolId);
        }
        var latest = repo.findLatestBySchoolId(schoolId);
        byte[] pdf;
        if (latest.isPresent()) {
            ReportCardDTO card = getReportCardUseCase.execute(schoolId, latest.get().getId());
            pdf = compose.render(ref, links.linker(schoolId, DocumentKind.REPORT_CARD, ref)
                    .apply(ReportCardDocumentData.of(card, this::dataUri)));
        } else {
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            if (school != null) {
                data.put("organisation", school.name());
                data.put("school", school.name());
                data.put("school_motto", school.motto() == null ? "" : school.motto());
                data.put("colors", com.moriba.skultem.application.services.BrandColors.of(school));
            }
            pdf = compose.preview(ref, data);
        }
        return new ReportFile("report-card-design-preview.pdf", PDF, pdf);
    }

    /** One student's card. */
    public ReportFile single(String schoolId, String id) {
        String template = requireTemplate(schoolId);
        ReportCardDTO card = getReportCardUseCase.execute(schoolId, id);
        var link = links.linker(schoolId, DocumentKind.REPORT_CARD, template);
        byte[] pdf = compose.render(template, link.apply(ReportCardDocumentData.of(card, this::dataUri)));
        return new ReportFile("report-card-" + safe(card.studentName()) + ".pdf", PDF, pdf);
    }

    /**
     * Every card of a class for a term, in one PDF, one student after another by name. scope picks which slice of
     * the term (see ReportCard.scopeKey): "ALL" (the whole term) unless given.
     */
    public ReportFile forClass(String schoolId, String classId, String termId, String scope) {
        String template = requireTemplate(schoolId);
        String scopeKey = scope == null || scope.isBlank() ? "ALL" : scope.trim();
        List<ReportCard> cards = repo.findAllBySchoolIdAndClassIdAndTermId(schoolId, classId, termId).stream()
                .filter(c -> scopeKey.equals(c.getScopeKey()))
                .sorted(Comparator.comparing(c -> c.getStudentName() == null ? "" : c.getStudentName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (cards.isEmpty()) {
            throw new NotFoundException("No report cards have been generated for this class and term yet");
        }
        if (cards.size() > MAX_CARDS) {
            throw new DocumentServiceException("That is more than " + MAX_CARDS + " report cards for one file");
        }
        List<Map<String, Object>> records = cards.stream()
                .map(c -> getReportCardUseCase.execute(schoolId, c.getId()))
                .map(card -> ReportCardDocumentData.of(card, this::dataUri))
                .map(links.linker(schoolId, DocumentKind.REPORT_CARD, template))
                .toList();
        byte[] pdf = compose.renderBatch(template, Map.of(), records);
        var first = cards.get(0);
        return new ReportFile("report-cards-" + safe(first.getClassName()) + "-" + safe(first.getTermName()) + ".pdf",
                PDF, pdf);
    }

    private String requireTemplate(String schoolId) {
        if (!isEnabled(schoolId)) {
            throw new DocumentServiceException("Report card PDFs from the document service are not set up yet");
        }
        return designs.templateFor(schoolId, DocumentKind.REPORT_CARD);
    }

    // A stored image as a data: URI, or null when it is not one of ours or cannot be read (the card then shows none).
    private String dataUri(String url) {
        try {
            return storage.downloadAsDataUri(url);
        } catch (RuntimeException e) {
            log.warn("Could not load an image for a report card: {}", e.getMessage());
            return null;
        }
    }

    private static String safe(String value) {
        String s = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9-_]+", "-").replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "document" : s;
    }
}
