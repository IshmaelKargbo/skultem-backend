package com.moriba.skultem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.moriba.skultem.application.dto.SchoolStructureDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.usecase.UpdateSchoolStructureUseCase.SectionInput;
import com.moriba.skultem.domain.model.ManagementSection;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.model.School.ManagementModel;
import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ManagementSectionRepository;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StaffManagementSectionRepository;
import com.moriba.skultem.domain.vo.Level;

import static com.moriba.skultem.domain.vo.Level.DAYCARE;
import static com.moriba.skultem.domain.vo.Level.JSS;
import static com.moriba.skultem.domain.vo.Level.NURSERY;
import static com.moriba.skultem.domain.vo.Level.PRIMARY;
import static com.moriba.skultem.domain.vo.Level.SSS;

@ExtendWith(MockitoExtension.class)
class UpdateSchoolStructureUseCaseTest {

    private static final String SCHOOL = "school-1";

    @Mock
    private SchoolRepository schoolRepo;
    @Mock
    private ClassRepository classRepo;
    @Mock
    private LogActivityUseCase logActivityUseCase;
    @Mock
    private StaffManagementSectionRepository staffSectionRepo;

    private final FakeLevels levels = new FakeLevels();
    private final FakeSections sections = new FakeSections();
    private School school;
    private UpdateSchoolStructureUseCase useCase;

    @BeforeEach
    void setUp() {
        school = School.create(SCHOOL, "King's Way", "kingsway", null, null);
        lenient().when(schoolRepo.findById(SCHOOL)).thenReturn(Optional.of(school));
        lenient().when(classRepo.countActiveBySchoolAndLevel(anyString(), any())).thenReturn(0);

        var get = new GetSchoolStructureUseCase(schoolRepo, levels, sections, classRepo);
        useCase = new UpdateSchoolStructureUseCase(schoolRepo, levels, sections, classRepo, staffSectionRepo, get,
                logActivityUseCase);
    }

    // ── Scenarios 1-3: unified schools of any shape ───────────────────────────

    @Test
    void daycareOnlyUnifiedSchool() {
        var res = useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(DAYCARE), List.of());

        assertThat(res.managementModel()).isEqualTo(ManagementModel.UNIFIED);
        assertThat(levelsOf(res)).containsExactly(DAYCARE);
        assertThat(res.sections()).isEmpty();
        assertThat(res.levels()).allMatch(l -> l.managementSectionId() == null);
    }

    @Test
    void nurseryAndPrimaryUnified() {
        var res = useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY, NURSERY), null);

        assertThat(levelsOf(res)).containsExactly(NURSERY, PRIMARY);
        assertThat(res.sections()).isEmpty();
    }

    @Test
    void primaryAndJssUnifiedIgnoresAnySectionsSent() {
        var res = useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY, JSS),
                List.of(new SectionInput(null, "Stray", List.of(PRIMARY))));

        assertThat(levelsOf(res)).containsExactly(PRIMARY, JSS);
        assertThat(res.sections()).isEmpty();
    }

    // ── Scenarios 4-5: section-based schools ─────────────────────────────────

    @Test
    void primaryJssTogetherAndSssSeparately() {
        var res = useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS, SSS), List.of(
                new SectionInput(null, "Primary & JSS", List.of(PRIMARY, JSS)),
                new SectionInput(null, "SSS", List.of(SSS))));

        assertThat(res.managementModel()).isEqualTo(ManagementModel.SECTION_BASED);
        assertThat(res.sections()).extracting(SchoolStructureDTO.ManagementSectionDTO::name)
                .containsExactly("Primary & JSS", "SSS");
        assertThat(res.sections().get(0).levels()).containsExactly(PRIMARY, JSS);
        assertThat(res.sections().get(1).levels()).containsExactly(SSS);
    }

    @Test
    void allFiveLevelsInThreeSections() {
        var res = useCase.execute(SCHOOL, ManagementModel.SECTION_BASED,
                List.of(DAYCARE, NURSERY, PRIMARY, JSS, SSS), List.of(
                        new SectionInput(null, "Early Years & Primary", List.of(DAYCARE, NURSERY, PRIMARY)),
                        new SectionInput(null, "JSS", List.of(JSS)),
                        new SectionInput(null, "SSS", List.of(SSS))));

        assertThat(res.sections()).hasSize(3);
        assertThat(res.sections().get(0).levels()).containsExactly(DAYCARE, NURSERY, PRIMARY);
        // One SchoolLevel row per level - never duplicated per section.
        assertThat(levels.rows).hasSize(5);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    @Test
    void rejectsNoLevels() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(), List.of()))
                .isInstanceOf(RuleException.class).hasMessageContaining("at least one school level");
    }

    @Test
    void sectionBasedRequiresEveryLevelAssigned() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS),
                List.of(new SectionInput(null, "Primary", List.of(PRIMARY)))))
                .isInstanceOf(RuleException.class).hasMessageContaining("not yet assigned: JSS");
    }

    @Test
    void levelCannotBelongToTwoSections() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS),
                List.of(new SectionInput(null, "A", List.of(PRIMARY, JSS)),
                        new SectionInput(null, "B", List.of(JSS)))))
                .isInstanceOf(RuleException.class).hasMessageContaining("only belong to one management section");
    }

    @Test
    void sectionCannotIncludeALevelTheSchoolDoesNotOffer() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY),
                List.of(new SectionInput(null, "A", List.of(PRIMARY, SSS)))))
                .isInstanceOf(RuleException.class).hasMessageContaining("doesn't offer");
    }

    @Test
    void sectionNamesAreUniqueIgnoringCase() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS),
                List.of(new SectionInput(null, "Lower", List.of(PRIMARY)),
                        new SectionInput(null, " lower ", List.of(JSS)))))
                .isInstanceOf(RuleException.class).hasMessageContaining("more than one management section");
    }

    @Test
    void sectionNeedsALevel() {
        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY),
                List.of(new SectionInput(null, "A", List.of(PRIMARY)), new SectionInput(null, "Empty", List.of()))))
                .isInstanceOf(RuleException.class).hasMessageContaining("needs at least one school level");
    }

    @Test
    void rejectsSectionIdFromAnotherSchool() {
        var foreign = ManagementSection.create("other-school", "Theirs", 0);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY),
                List.of(new SectionInput(foreign.getId(), "Theirs", List.of(PRIMARY)))))
                .isInstanceOf(RuleException.class).hasMessageContaining("not found");
    }

    @Test
    void cannotRemoveALevelThatStillHasClasses() {
        useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY, JSS), List.of());
        when(classRepo.countActiveBySchoolAndLevel(eq(SCHOOL), eq(JSS))).thenReturn(2);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY), List.of()))
                .isInstanceOf(RuleException.class).hasMessageContaining("JSS still has 2 classes");
    }

    // ── Editing ──────────────────────────────────────────────────────────────

    @Test
    void renamingKeepsTheSectionIdAndRemovedSectionsAreDeleted() {
        var first = useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS, SSS), List.of(
                new SectionInput(null, "Primary", List.of(PRIMARY)),
                new SectionInput(null, "JSS", List.of(JSS)),
                new SectionInput(null, "SSS", List.of(SSS))));
        String primaryId = first.sections().get(0).id();

        var res = useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS, SSS), List.of(
                new SectionInput(primaryId, "Primary & JSS", List.of(PRIMARY, JSS)),
                new SectionInput(null, "Senior", List.of(SSS))));

        assertThat(res.sections()).hasSize(2);
        assertThat(res.sections().get(0).id()).isEqualTo(primaryId);
        assertThat(res.sections().get(0).name()).isEqualTo("Primary & JSS");
        assertThat(sections.rows).hasSize(2);
    }

    @Test
    void switchingToUnifiedRemovesAllSections() {
        useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS), List.of(
                new SectionInput(null, "Primary", List.of(PRIMARY)),
                new SectionInput(null, "JSS", List.of(JSS))));

        var res = useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY, JSS), List.of());

        assertThat(res.sections()).isEmpty();
        assertThat(sections.rows).isEmpty();
        assertThat(res.levels()).allMatch(l -> l.managementSectionId() == null);
        assertThat(school.getManagementModel()).isEqualTo(ManagementModel.UNIFIED);
    }

    @Test
    void cannotRemoveASectionStaffAreLimitedTo() {
        var first = useCase.execute(SCHOOL, ManagementModel.SECTION_BASED, List.of(PRIMARY, JSS), List.of(
                new SectionInput(null, "Primary", List.of(PRIMARY)),
                new SectionInput(null, "JSS", List.of(JSS))));
        String jssId = first.sections().get(1).id();
        // Order matters: Mockito matches the most-recently-defined stub first, so the specific
        // jssId answer must come after the catch-all - otherwise the Primary section's (unstubbed)
        // id trips strict-stubbing's "argument mismatch" check instead of just returning false.
        when(staffSectionRepo.existsBySectionId(anyString())).thenReturn(false);
        when(staffSectionRepo.existsBySectionId(jssId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(SCHOOL, ManagementModel.UNIFIED, List.of(PRIMARY, JSS), List.of()))
                .isInstanceOf(RuleException.class).hasMessageContaining("Staff are still limited to \"JSS\"");
        assertThat(sections.rows).hasSize(2);
    }

    private static List<Level> levelsOf(SchoolStructureDTO res) {
        return res.levels().stream().map(SchoolStructureDTO.LevelDTO::level).toList();
    }

    // ── In-memory repositories ───────────────────────────────────────────────

    private static class FakeLevels implements SchoolLevelRepository {
        final Map<String, SchoolLevel> rows = new LinkedHashMap<>();

        public void save(SchoolLevel domain) {
            rows.put(domain.getId(), domain);
        }

        public List<SchoolLevel> findBySchoolId(String schoolId) {
            return rows.values().stream().filter(r -> r.getSchoolId().equals(schoolId))
                    .sorted(Comparator.comparing(SchoolLevel::getLevel)).toList();
        }

        public void delete(SchoolLevel domain) {
            rows.remove(domain.getId());
        }
    }

    private static class FakeSections implements ManagementSectionRepository {
        final Map<String, ManagementSection> rows = new LinkedHashMap<>();

        public void save(ManagementSection domain) {
            rows.put(domain.getId(), domain);
        }

        public List<ManagementSection> findBySchoolId(String schoolId) {
            return new ArrayList<>(rows.values().stream().filter(r -> r.getSchoolId().equals(schoolId))
                    .sorted(Comparator.comparingInt(ManagementSection::getDisplayOrder)).toList());
        }

        public void delete(ManagementSection domain) {
            rows.remove(domain.getId());
        }
    }
}
