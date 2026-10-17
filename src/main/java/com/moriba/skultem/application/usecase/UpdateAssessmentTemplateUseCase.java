package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public record Item(String id, String name, Integer weight) {
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
        var cyclesBefore = cycleRepo.findAllByTemplate(templateId);
        for (int i = 0; i < wanted.size(); i++) {
            var item = wanted.get(i);
            int position = i + 1;
            var match = matches.get(i);
            if (match != null) {
                boolean weightChanged = !match.getWeight().equals(item.weight());
                match.update(item.name(), item.weight(), position);
                assessmentRepo.save(match);
                if (weightChanged) {
                    maintenance.updateScoreWeight(match.getId(), item.weight());
                    reweighted++;
                }
            } else {
                var fresh = Assessment.create(UUID.randomUUID().toString(), schoolId, template, item.name(),
                        item.weight(), position);
                assessmentRepo.save(fresh);
                created.add(fresh);
                added++;
            }
        }

        for (var gone : removed) {
            maintenance.deleteAssessmentCascade(gone.getId());
        }

        for (var fresh : created) {
            addToExistingTests(schoolId, cyclesBefore, fresh);
        }

        var list = assessmentRepo.findAllByTemplateIdAndSchoolId(templateId, schoolId).stream()
                .map(AssessmentMapper::toDTO).toList();
        var summary = "Template " + template.getName() + " updated: " + added + " test(s) added, " + reweighted
                + " re-weighted, " + removed.size() + " removed" + (force ? " (forced)" : "")
                + "; used by " + usage.classNames().size() + " class(es), " + usage.submittedTests()
                + " submitted test(s), " + usage.markedScores() + " marked score(s)";
        return new Result(AssessmentTemplateMapper.toDTO(template, list), summary);
    }

    // A new test appears, locked, for every class subject and term that already has tests from this template.
    private void addToExistingTests(String schoolId, List<ClassSubjectAssessmentLifeCycle> cyclesBefore,
            Assessment fresh) {
        Map<String, ClassSubjectAssessmentLifeCycle> groups = new LinkedHashMap<>();
        for (var c : cyclesBefore) {
            groups.putIfAbsent(c.getSubject().getId() + "|" + c.getTerm().getId(), c);
        }

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
        int total = 0;
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
            total += item.weight();
            clean.add(new Item(item.id(), name, item.weight()));
        }
        if (total > 100) {
            throw new RuleException("Total assessment weight cannot exceed 100%");
        }
        return clean;
    }
}
