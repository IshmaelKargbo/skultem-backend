package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.error.DocumentServiceException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.DocumentDesignService;
import com.moriba.skultem.application.services.ExternalExamDocumentData;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.application.services.SchoolBrandingResolver;
import com.moriba.skultem.domain.model.ExternalExamCandidate;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.infrastructure.bucket.R2StorageService;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.report.ReportFile;

/**
 * External exam result slips and statements of result, made by M-Compose from confirmed results, in the designs the
 * school picked (DocumentDesignService), else m-compose.templates.exam-result-slip / exam-statement.
 */
@Service
public class RenderExternalExamDocumentUseCase {
    private static final Logger log = LoggerFactory.getLogger(RenderExternalExamDocumentUseCase.class);

    public enum Kind {
        SLIP("result-slip"), STATEMENT("statement-of-result");

        final String fileName;

        Kind(String fileName) {
            this.fileName = fileName;
        }

        public static Kind of(String value) {
            if (value == null || value.isBlank() || value.equalsIgnoreCase("slip")) {
                return SLIP;
            }
            if (value.equalsIgnoreCase("statement")) {
                return STATEMENT;
            }
            throw new RuleException("The document is either slip or statement");
        }
    }

    private final ExternalExamUseCase exams;
    private final ClassRepository classRepo;
    private final AcademicYearRepository academicYearRepo;
    private final SchoolRepository schoolRepo;
    private final StudentRepository studentRepo;
    private final SchoolBrandingResolver branding;
    private final R2StorageService storage;
    private final MComposeClient compose;
    private final DocumentDesignService designs;
    private final com.moriba.skultem.application.services.DocumentFieldLinkService links;

    public RenderExternalExamDocumentUseCase(ExternalExamUseCase exams, ClassRepository classRepo,
            AcademicYearRepository academicYearRepo, SchoolRepository schoolRepo, StudentRepository studentRepo,
            SchoolBrandingResolver branding, R2StorageService storage, MComposeClient compose,
            DocumentDesignService designs, com.moriba.skultem.application.services.DocumentFieldLinkService links) {
        this.exams = exams;
        this.classRepo = classRepo;
        this.academicYearRepo = academicYearRepo;
        this.schoolRepo = schoolRepo;
        this.studentRepo = studentRepo;
        this.branding = branding;
        this.storage = storage;
        this.compose = compose;
        this.designs = designs;
        this.links = links;
    }

    // On when M-Compose is set up; an exam may have its own designs even without general ones, so a missing design
    // is reported when printing.
    public boolean isEnabled(String schoolId) {
        return compose.isEnabled();
    }

    /** One candidate's document. */
    @Transactional(readOnly = true)
    public ReportFile candidate(String schoolId, String sittingId, String candidateId, Kind kind) {
        var ctx = context(schoolId, sittingId);
        var candidate = ctx.candidates.stream().filter(c -> c.getId().equals(candidateId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Candidate not found"));
        String template = template(schoolId, ctx.exam, kind);
        byte[] pdf = compose.render(template, links.linker(schoolId, docKind(kind), template).apply(data(ctx, candidate)));
        return new ReportFile(kind.fileName + "-" + safe(candidate.getIndexNumber()) + "-"
                + safe(candidate.getStudentName()) + ".pdf", "application/pdf", pdf);
    }

    /** Every candidate's document, one after another, in one PDF. */
    @Transactional(readOnly = true)
    public ReportFile all(String schoolId, String sittingId, Kind kind) {
        var ctx = context(schoolId, sittingId);
        if (ctx.candidates.isEmpty()) {
            throw new RuleException("This exam has no candidates");
        }
        String template = template(schoolId, ctx.exam, kind);
        var link = links.linker(schoolId, docKind(kind), template);
        List<Map<String, Object>> records = ctx.candidates.stream().map(c -> link.apply(data(ctx, c))).toList();
        byte[] pdf = compose.renderBatch(template, Map.of(), records);
        return new ReportFile(kind.fileName + "s-" + safe(ctx.exam.getCode()) + "-" + safe(ctx.className) + "-"
                + safe(ctx.year) + ".pdf", "application/pdf", pdf);
    }

    private record Context(com.moriba.skultem.application.dto.SchoolDTO school,
            com.moriba.skultem.domain.model.ExternalExam exam,
            com.moriba.skultem.domain.model.ExternalExamSitting sitting, String className, String year,
            List<ExternalExamCandidate> candidates) {
    }

    private Context context(String schoolId, String sittingId) {
        if (!isEnabled(schoolId)) {
            throw new DocumentServiceException("Printing exam results is not set up on this server yet");
        }
        var sitting = exams.findSitting(schoolId, sittingId);
        if (!sitting.isConfirmed()) {
            throw new RuleException("Confirm these results before printing them");
        }
        var exam = exams.exam(schoolId, sitting.getExamId());
        var clazz = classRepo.findByIdAndSchool(sitting.getClassId(), schoolId).orElse(null);
        var school = schoolRepo.findById(schoolId)
                .map(s -> branding.schoolDtoFor(s, clazz == null ? null : clazz.getLevel())).orElse(null);
        String year = academicYearRepo.findByIdAndSchoolId(sitting.getAcademicYearId(), schoolId)
                .map(y -> y.getName()).orElse("");
        return new Context(school, exam, sitting, clazz == null ? "" : clazz.getName(), year,
                exams.candidates(sittingId));
    }

    private Map<String, Object> data(Context ctx, ExternalExamCandidate c) {
        var student = studentRepo.findByIdAndSchoolId(c.getStudentId(), ctx.sitting.getSchoolId()).orElse(null);
        return ExternalExamDocumentData.of(ctx.school, ctx.exam, ctx.sitting, ctx.className, ctx.year, c, student,
                this::dataUri);
    }

    private static DocumentKind docKind(Kind kind) {
        return kind == Kind.STATEMENT ? DocumentKind.EXAM_STATEMENT : DocumentKind.EXAM_RESULT_SLIP;
    }

    // The exam's own design, else the school's general exam design, else the server's.
    private String template(String schoolId, com.moriba.skultem.domain.model.ExternalExam exam, Kind kind) {
        String t = designs.examTemplateFor(schoolId, exam,
                kind == Kind.STATEMENT ? DocumentKind.EXAM_STATEMENT : DocumentKind.EXAM_RESULT_SLIP);
        if (t == null || t.isBlank()) {
            throw new DocumentServiceException("Choose a print design for " + exam.getCode() + " results first");
        }
        return t;
    }

    private String dataUri(String url) {
        try {
            return storage.downloadAsDataUri(url);
        } catch (RuntimeException e) {
            log.warn("Could not load an image for an exam result: {}", e.getMessage());
            return null;
        }
    }

    private static String safe(String value) {
        String s = value == null ? "" : value.trim().replaceAll("[^A-Za-z0-9-_]+", "-").replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "document" : s;
    }
}
