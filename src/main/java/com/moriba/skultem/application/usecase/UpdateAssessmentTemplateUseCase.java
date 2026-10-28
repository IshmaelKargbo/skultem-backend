package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentTemplateDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.application.mapper.AssessmentTemplateMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.StudentAssessmentRepository;
import com.moriba.skultem.domain.repository.TemplateMaintenanceRepository;
import com.moriba.skultem.domain.service.TermWeights;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Edits a template in place, even after classes have used it, and carries the change through to what already exists:
//  - a renamed or re-weighted test keeps its tests and marks; every existing score takes the new weight,
//  - a new test is added for every student of every class that uses the template (locked, until its turn),
//  - a removed test is deleted with its tests and scores.
// Once something has been submitted for approval, the change is refused unless it is forced - the caller shows the usage
// first and asks. Removing a test that already has marks needs the same confirmation.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateAssessmentTemplateUseCase {

    public record Details(String name, Integer passMark, String description) {
    }

    // `id` is null for a test that is new (or when the caller only knows names - then the name is matched).
    // `terms`: the term numbers the test runs in; empty = every term.
    public record Item(String id, String name, Integer weight, Set<Integer> terms) {
        public Item(String id, String name, Integer weight) {
            this(id, name, weight, Set.of());
        }
    }

    public record Result(AssessmentTemplateDTO template, String summary) {
        @Override
        public String toString() {
            return summary;
        }
    }

    private final AssessmentTemplateRepository templateRepo;
    private final AssessmentRepository assessmentRepo;
    private final AssessmentScoreRepository scoreRepo;
    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final StudentAssessmentRepository studentAssessmentRepo;
    private final TemplateMaintenanceRepository maintenance;

    @AuditLogAnnotation(action = "ASSESSMENT_TEMPLATE_UPDATED")
    public Result execute(String schoolId, String templateId, Details details, List<Item> items, boolean force) {
        var template = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        var wanted = sanitize(items);
        var usage = maintenance.usage(schoolId, templateId);
        if (usage.submittedTests() > 0 && !force) {
            throw new RuleException(usage.submittedTests() + " test" + (usage.submittedTests() == 1 ? " has" : "s have")
                    + " already been submitted for approval in " + usage.classNames().size() + " class"
                    + (usage.classNames().size() == 1 ? "" : "es") + ". Confirm to change the template anyway.");
        }

        var existing = assessmentRepo.findAllByTemplateIdAndSchoolId(templateId, schoolId);
        Map<String, Assessment> byId = new LinkedHashMap<>();
        Map<String, Assessment> byName = new LinkedHashMap<>();
        existing.forEach(a -> {
            byId.put(a.getId(), a);
            byName.put(a.getName().toLowerCase(Locale.ROOT), a);
        });

        // Match every wanted test to an existing one: by id, else by name.
        var kept = new HashSet<String>();
        var matches = new ArrayList<Assessment>();
        for (var item : wanted) {
            Assessment match = item.id() != null ? byId.get(item.id()) : null;
            if (match == null) {
                var byNameMatch = byName.get(item.name().toLowerCase(Locale.ROOT));
                match = byNameMatch != null && !kept.contains(byNameMatch.getId()) ? byNameMatch : null;
            }
            if (match != null) {
                kept.add(match.getId());
            }
            matches.add(match);
        }

        // Tests that are no longer wanted: refuse to drop marks unless forced.
        var removed = existing.stream().filter(a -> !kept.contains(a.getId())).toList();
        for (var gone : removed) {
            if (!force && maintenance.markedScoresOf(gone.getId()) > 0) {
                throw new RuleException(gone.getName() + " already has marks, and removing it deletes them. "
                        + "Confirm to remove it anyway.");
            }
        }

        // Kept tests taken out of some terms: their tests in those terms go. Only tests still waiting for their turn
        // can go; one that has opened is refused, and one with marks needs the same confirmation as a removal.
        var cyclesBefore = cycleRepo.findAllByTemplate(templateId);
        Map<String, Set<Integer>> droppedTerms = new LinkedHashMap<>();
        for (int i = 0; i < wanted.size(); i++) {
            var match = matches.get(i);
            if (match == null) {
                continue;
            }
            var item = wanted.get(i);
            var drop = new TreeSet<Integer>();
            for (var c : cyclesBefore) {
                int term = c.getTerm().getTermNumber();
                if (c.getAssessment().getId().equals(match.getId()) && !runsIn(item.terms(), term)) {
                    if (c.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED) {
                        throw new RuleException(item.name() + " has already opened in term " + term
                                + ", so it can't be taken out of that term. Finish the term first.");
                    }
                    drop.add(term);
                }
            }
            if (drop.isEmpty()) {
                continue;
            }
            if (!force && maintenance.markedScoresOf(match.getId(), drop) > 0) {
                throw new RuleException(item.name() + " already has marks in term " + joinTerms(drop)
                        + ", and taking it out of that term deletes them. Confirm to change it anyway.");
            }
            droppedTerms.put(match.getId(), drop);
        }

        // Details
        if (details != null && details.name() != null && !details.name().isBlank()) {
            if (details.passMark() == null || details.passMark() < 0 || details.passMark() > 100) {
                throw new RuleException("Pass mark must be between 0 and 100");
            }
            template.updateDetails(details.name().trim(), details.passMark(),
                    details.description() == null ? template.getDescription() : details.description().trim());
            templateRepo.save(template);
        }

        int reweighted = 0, added = 0;
        var created = new ArrayList<Assessment>();
        var widened = new ArrayList<Assessment>();
        for (int i = 0; i < wanted.size(); i++) {
            var item = wanted.get(i);
            int position = i + 1;
            var match = matches.get(i);
            if (match != null) {
                boolean weightChanged = !match.getWeight().equals(item.weight());
                boolean termsChanged = !match.getTerms().equals(item.terms());
                match.update(item.name(), item.weight(), position, item.terms());
                if (termsChanged) {
                    widened.add(match);
                }
                assessmentRepo.save(match);
                if (weightChanged) {
                    maintenance.updateScoreWeight(match.getId(), item.weight());
                    reweighted++;
                }
            } else {
                var fresh = Assessment.create(UUID.randomUUID().toString(), schoolId, template, item.name(),
                        item.weight(), position, item.terms());
                assessmentRepo.save(fresh);
                created.add(fresh);
                added++;
            }
        }

        for (var gone : removed) {
            maintenance.deleteAssessmentCascade(gone.getId());
        }

        droppedTerms.forEach(maintenance::deleteAssessmentInTerms);

        for (var fresh : created) {
            addToExistingTests(schoolId, cyclesBefore, fresh);
        }
        // A kept test that now runs in more terms appears in those terms too.
        for (var test : widened) {
            addToExistingTests(schoolId, cyclesBefore, test);
        }

        var list = assessmentRepo.findAllByTemplateIdAndSchoolId(templateId, schoolId).stream()
                .map(AssessmentMapper::toDTO).toList();
        var summary = "Template " + template.getName() + " updated: " + added + " test(s) added, " + reweighted
                + " re-weighted, " + removed.size() + " removed" + (force ? " (forced)" : "")
                + "; used by " + usage.classNames().size() + " class(es), " + usage.submittedTests()
                + " submitted test(s), " + usage.markedScores() + " marked score(s)";
        return new Result(AssessmentTemplateMapper.toDTO(template, list), summary);
    }

    // A test appears, locked, for every class subject and term that already has tests from this template - in the
    // terms the test runs in, where it isn't there yet.
    private void addToExistingTests(String schoolId, List<ClassSubjectAssessmentLifeCycle> cyclesBefore,
            Assessment fresh) {
        Map<String, ClassSubjectAssessmentLifeCycle> groups = new LinkedHashMap<>();
        var present = new HashSet<String>();
        for (var c : cyclesBefore) {
            var key = c.getSubject().getId() + "|" + c.getTerm().getId();
            if (c.getAssessment().getId().equals(fresh.getId())) {
                present.add(key);
            }
            if (fresh.runsInTerm(c.getTerm().getTermNumber())) {
                groups.putIfAbsent(key, c);
            }
        }
        present.forEach(groups::remove);

        var cycles = new ArrayList<ClassSubjectAssessmentLifeCycle>();
        var scores = new ArrayList<AssessmentScore>();
        for (var sample : groups.values()) {
            var cycle = ClassSubjectAssessmentLifeCycle.create(UUID.randomUUID().toString(), schoolId,
                    sample.getSubject(), sample.getTerm(), fresh, ClassSubjectAssessmentLifeCycle.Status.LOCKED);
            cycles.add(cycle);
            var students = studentAssessmentRepo.findAllBySubjectAndSessionAndTermId(
                    sample.getSubject().getSubject().getId(), sample.getSubject().getSession().getId(),
                    sample.getTerm().getId());
            for (var sa : students) {
                scores.add(AssessmentScore.create(UUID.randomUUID().toString(), schoolId, sa, cycle,
                        fresh.getWeight()));
            }
        }
        if (!cycles.isEmpty()) {
            cycleRepo.saveAll(cycles);
            scoreRepo.saveAll(scores);
        }
    }

    private List<Item> sanitize(List<Item> items) {
        if (items == null || items.isEmpty()) {
            throw new RuleException("At least one assessment is required");
        }
        var clean = new ArrayList<Item>();
        var names = new HashSet<String>();
        for (var item : items) {
            if (item == null || item.name() == null || item.name().isBlank()) {
                throw new RuleException("Every assessment needs a name");
            }
            if (item.weight() == null || item.weight() <= 0 || item.weight() > 100) {
                throw new RuleException("Each assessment weight must be between 1 and 100");
            }
            var name = item.name().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new RuleException("Duplicate assessment: " + name);
            }
            var terms = new TreeSet<Integer>();
            if (item.terms() != null) {
                for (var t : item.terms()) {
                    if (t == null || t < 1 || t > Assessment.MAX_TERM) {
                        throw new RuleException("A test can only run in terms 1 to " + Assessment.MAX_TERM);
                    }
                    terms.add(t);
                }
            }
            clean.add(new Item(item.id(), name, item.weight(), terms));
        }
        var overweight = TermWeights.overweight(clean.stream()
                .map(i -> new TermWeights.Line(i.name(), i.weight(), i.terms())).toList());
        if (overweight != null) {
            throw new RuleException(overweight);
        }
        return clean;
    }

    private static boolean runsIn(Set<Integer> terms, int term) {
        return terms == null || terms.isEmpty() || terms.contains(term);
    }

    private static String joinTerms(Set<Integer> terms) {
        return terms.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
    }
}
