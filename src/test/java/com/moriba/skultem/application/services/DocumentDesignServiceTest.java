package com.moriba.skultem.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.repository.SchoolDocumentDesignRepository;
import com.moriba.skultem.domain.vo.DocumentKind;
import com.moriba.skultem.infrastructure.compose.MComposeClient;
import com.moriba.skultem.infrastructure.compose.MComposeClient.Design;

class DocumentDesignServiceTest {

    private final Map<String, String> stored = new HashMap<>();
    private final MComposeClient compose = mock(MComposeClient.class);
    private DocumentDesignService service;

    @BeforeEach
    void setUp() {
        SchoolDocumentDesignRepository repo = new SchoolDocumentDesignRepository() {
            public Optional<String> find(String schoolId, DocumentKind kind) {
                return Optional.ofNullable(stored.get(schoolId + kind));
            }

            public void save(String schoolId, DocumentKind kind, String template, String updatedBy) {
                stored.put(schoolId + kind, template);
            }

            public void delete(String schoolId, DocumentKind kind) {
                stored.remove(schoolId + kind);
            }
        };
        when(compose.designs()).thenReturn(List.of(
                new Design("sample:certificate", "certificate", "Certificate", "", "Certificate", true, true, 0),
                new Design("sample:report-card-classic", "report-card-classic", "Report card: classic", "",
                        "Report card", true, true, 0),
                new Design("tpl-9", "tpl-9", "Report card: our school", "", "Report card", false, false, 3)));
        when(compose.fieldsOf(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            Design d = inv.getArgument(0);
            // The school's design uses one field Skultem doesn't have.
            return d.readyMade() ? java.util.Set.of("student", "results") : java.util.Set.of("student", "attitude");
        });
        service = new DocumentDesignService(repo, compose, "sample:report-card-classic", "sample:exam-result-slip",
                "sample:exam-statement-of-result");
    }

    @Test
    void theServerDefaultUntilTheSchoolPicksOne() {
        assertThat(service.templateFor("s1", DocumentKind.REPORT_CARD)).isEqualTo("sample:report-card-classic");

        service.choose("s1", DocumentKind.REPORT_CARD, "tpl-9", "u1");

        assertThat(service.templateFor("s1", DocumentKind.REPORT_CARD)).isEqualTo("tpl-9");
        // Another school is not affected.
        assertThat(service.templateFor("s2", DocumentKind.REPORT_CARD)).isEqualTo("sample:report-card-classic");
    }

    @Test
    void clearingGoesBackToTheDefault() {
        service.choose("s1", DocumentKind.REPORT_CARD, "tpl-9", "u1");
        var choice = service.choose("s1", DocumentKind.REPORT_CARD, "", "u1");

        assertThat(choice.current()).isEqualTo("sample:report-card-classic");
        assertThat(choice.chosenBySchool()).isFalse();
    }

    @Test
    void listsTheDesignsMadeForTheDocumentReadyMadeAfterTheSchoolsOwn() {
        var choice = service.choice("s1", DocumentKind.REPORT_CARD, false);

        assertThat(choice.options()).extracting(DocumentDesignService.Option::ref)
                .containsExactly("sample:report-card-classic", "tpl-9");
        assertThat(service.choice("s1", DocumentKind.REPORT_CARD, true).options()).hasSize(3);
    }

    @Test
    void anExamUsesItsOwnDesignElseTheSchoolsGeneralOneElseTheServers() {
        var d = ExternalExamDefaults.all().get(1); // BECE
        var bece = com.moriba.skultem.domain.model.ExternalExam.create("e", "s1", d.code(), d.name(), d.examBody(),
                d.level(), d.scale(), d.mode(), d.bestOf(), true);

        assertThat(service.examTemplateFor("s1", bece, DocumentKind.EXAM_RESULT_SLIP)).isEqualTo("sample:exam-result-slip");

        service.choose("s1", DocumentKind.EXAM_RESULT_SLIP, "tpl-9", "u1");
        assertThat(service.examTemplateFor("s1", bece, DocumentKind.EXAM_RESULT_SLIP)).isEqualTo("tpl-9");

        bece.setDesign(false, "sample:certificate");
        assertThat(service.examTemplateFor("s1", bece, DocumentKind.EXAM_RESULT_SLIP)).isEqualTo("sample:certificate");
        // The statement has no design of its own, so it still follows the general one.
        assertThat(service.examTemplateFor("s1", bece, DocumentKind.EXAM_STATEMENT))
                .isEqualTo("sample:exam-statement-of-result");
        var choice = service.examChoice("s1", bece, DocumentKind.EXAM_RESULT_SLIP, false);
        assertThat(choice.chosenBySchool()).isTrue();
        assertThat(choice.serverDefault()).isEqualTo("tpl-9");
    }

    @Test
    void saysWhichFieldsADesignLeavesBlankAndPutsTheBestFitFirst() {
        var options = service.choice("s1", DocumentKind.REPORT_CARD, false).options();

        assertThat(options).extracting(DocumentDesignService.Option::ref)
                .containsExactly("sample:report-card-classic", "tpl-9");
        assertThat(options.get(0).missing()).isEmpty();
        assertThat(options.get(1).missing()).containsExactly("attitude");
        assertThat(options.get(1).fields()).isEqualTo(2);
    }

    @Test
    void examStatementsAlsoOfferCertificatesAndEverySavedDesign() {
        var refs = service.choice("s1", DocumentKind.EXAM_STATEMENT, false).options().stream()
                .map(DocumentDesignService.Option::ref).toList();
        assertThat(refs).contains("sample:certificate", "sample:report-card-classic", "tpl-9");
    }

    @Test
    void aDeletedDesignFallsBackToATemplateThatExists() {
        when(compose.isEnabled()).thenReturn(true);
        // The server's default names a template that was deleted from the engine.
        var withDeadDefault = new DocumentDesignService(new SchoolDocumentDesignRepository() {
            public Optional<String> find(String schoolId, DocumentKind kind) {
                return Optional.empty();
            }

            public void save(String schoolId, DocumentKind kind, String template, String updatedBy) {
            }

            public void delete(String schoolId, DocumentKind kind) {
            }
        }, compose, "navy-deleted", "", "");

        assertThat(withDeadDefault.templateFor("s1", DocumentKind.REPORT_CARD)).isEqualTo("tpl-9");
        // A school's pick that still exists is kept.
        stored.put("s1" + DocumentKind.REPORT_CARD, "tpl-9");
        assertThat(service.templateFor("s1", DocumentKind.REPORT_CARD)).isEqualTo("tpl-9");
    }

    @Test
    void refusesADesignTheEngineDoesNotHave() {
        assertThatThrownBy(() -> service.choose("s1", DocumentKind.REPORT_CARD, "gone", "u1"))
                .isInstanceOf(RuleException.class);
        assertThat(stored).isEmpty();
    }
}
