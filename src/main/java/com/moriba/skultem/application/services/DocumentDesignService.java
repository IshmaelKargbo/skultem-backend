package com.moriba.skultem.application.services;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ExternalExam;
import com.moriba.skultem.domain.repository.SchoolDocumentDesignRepository;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.infrastructure.compose.MComposeClient;

/**
 * Which M-Compose design a school prints each kind of document with. A school picks one from the designs on the
 * engine (its ready-made ones and the templates saved in Skultem's account); until it does, the server's default
 * for that kind applies (m-compose.templates.*).
 */
@Service
public class DocumentDesignService {

    /**
     * A design as the picker shows it. ref is what gets stored and rendered ("sample:<id>" or a saved id).
     * fields / missing: how many data fields the design has, and the ones Skultem doesn't fill (they print blank);
     * null when they couldn't be read.
     */
    public record Option(String ref, String name, String description, String category, boolean readyMade,
            boolean hasPreview, boolean suggested, Integer fields, List<String> missing) {
    }

    public record Choice(String kind, String current, String serverDefault, boolean chosenBySchool,
            List<Option> options) {
    }

    private final SchoolDocumentDesignRepository repo;
    private final MComposeClient compose;
    private final Map<DocumentKind, String> defaults = new EnumMap<>(DocumentKind.class);

    public DocumentDesignService(SchoolDocumentDesignRepository repo, MComposeClient compose,
            @Value("${m-compose.templates.report-card:}") String reportCard,
            @Value("${m-compose.templates.exam-result-slip:sample:exam-result-slip}") String examSlip,
            @Value("${m-compose.templates.exam-statement:sample:exam-statement-of-result}") String examStatement) {
        this.repo = repo;
        this.compose = compose;
        defaults.put(DocumentKind.REPORT_CARD, trim(reportCard));
        defaults.put(DocumentKind.EXAM_RESULT_SLIP, trim(examSlip));
        defaults.put(DocumentKind.EXAM_STATEMENT, trim(examStatement));
    }

    /** The design to print with: the school's choice, else the server default ("" when there is neither). */
    public String templateFor(String schoolId, DocumentKind kind) {
        if (schoolId != null) {
            var chosen = repo.find(schoolId, kind);
            if (chosen.isPresent() && !chosen.get().isBlank()) {
                return usable(kind, chosen.get());
            }
        }
        return usable(kind, defaults.get(kind));
    }

    /**
     * The design itself when it is on the engine, else (it was deleted, or the setting names one that never existed)
     * the first template saved for this kind of document, so documents keep printing. A ready-made "sample:" design,
     * or an engine that can't be asked right now, leaves the design as it is.
     */
    private String usable(DocumentKind kind, String ref) {
        if (ref != null && ref.startsWith("sample:")) {
            return ref;
        }
        List<MComposeClient.Design> all;
        try {
            all = compose.isEnabled() ? compose.designs() : List.of();
        } catch (RuntimeException e) {
            return ref == null ? "" : ref;
        }
        if (ref != null && !ref.isBlank() && all.stream().anyMatch(d -> d.ref().equals(ref))) {
            return ref;
        }
        return all.stream().filter(d -> kind.suits(d.category()))
                .sorted(Comparator.comparing(MComposeClient.Design::readyMade)
                        .thenComparing(MComposeClient.Design::name, String.CASE_INSENSITIVE_ORDER))
                .map(MComposeClient.Design::ref).findFirst()
                .orElse(ref == null ? "" : ref);
    }

    /** The design an exam's slip or statement prints with: the exam's own, else the school's general one. */
    public String examTemplateFor(String schoolId, ExternalExam exam, DocumentKind kind) {
        String own = exam == null ? null : exam.design(kind == DocumentKind.EXAM_STATEMENT);
        // An exam's own design that has been deleted from the engine falls back to the school's general one.
        if (own != null && (own.startsWith("sample:") || existsOnEngine(own))) {
            return own;
        }
        return templateFor(schoolId, kind);
    }

    /**
     * An exam's design for its slip or statement and the designs it can pick. serverDefault is what it prints with
     * when it has none of its own (the school's general design); chosenBySchool says it has its own.
     */
    public Choice examChoice(String schoolId, ExternalExam exam, DocumentKind kind, boolean all) {
        String own = exam.design(kind == DocumentKind.EXAM_STATEMENT);
        return new Choice(kind.key(), examTemplateFor(schoolId, exam, kind), templateFor(schoolId, kind),
                own != null, options(kind, all));
    }

    private boolean existsOnEngine(String ref) {
        try {
            return !compose.isEnabled() || compose.designs().stream().anyMatch(d -> d.ref().equals(ref));
        } catch (RuntimeException e) {
            return true; // can't tell right now: keep it
        }
    }

    /** Throws unless the engine has this design. */
    public void requireDesign(String ref) {
        if (compose.designs().stream().noneMatch(d -> d.ref().equals(ref))) {
            throw new RuleException("That design is not on the document service any more. Pick another.");
        }
    }

    /** The school's current design and every design it can pick (the ones made for this kind first). */
    public Choice choice(String schoolId, DocumentKind kind, boolean all) {
        String chosen = repo.find(schoolId, kind).orElse(null);
        return new Choice(kind.key(), templateFor(schoolId, kind), defaults.get(kind), chosen != null,
                options(kind, all));
    }

    // The designs for this kind: the ones made for it, and every design saved in Skultem's account (they were made
    // for schools); all=true adds the rest. Best fitting first, the school's own before the ready-made ones.
    private List<Option> options(DocumentKind kind, boolean all) {
        var provided = providedFields(kind);
        return compose.designs().stream()
                .filter(d -> all || kind.suits(d.category()) || !d.readyMade())
                .map(d -> {
                    var fields = kind.suits(d.category()) || !d.readyMade() ? compose.fieldsOf(d) : null;
                    List<String> missing = fields == null ? null
                            : fields.stream().filter(f -> !provided.contains(f)).sorted().toList();
                    return new Option(d.ref(), d.name(), d.description(), d.category(), d.readyMade(),
                            d.hasPreview(), kind.suits(d.category()), fields == null ? null : fields.size(), missing);
                })
                .sorted(Comparator.comparing(Option::suggested).reversed()
                        .thenComparing(o -> o.missing() == null ? Integer.MAX_VALUE : o.missing().size())
                        .thenComparing(Option::readyMade)
                        .thenComparing(Option::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** The data fields Skultem fills in for this kind of document. */
    public static java.util.Set<String> providedFields(DocumentKind kind) {
        return kind == DocumentKind.REPORT_CARD ? ReportCardDocumentData.FIELDS : ExternalExamDocumentData.FIELDS;
    }

    /** The school prints this kind with this design from now on; null or blank goes back to the server default. */
    public Choice choose(String schoolId, DocumentKind kind, String template, String userId) {
        if (template == null || template.isBlank()) {
            repo.delete(schoolId, kind);
        } else {
            String ref = template.trim();
            requireDesign(ref);
            repo.save(schoolId, kind, ref, userId);
        }
        return choice(schoolId, kind, true);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
