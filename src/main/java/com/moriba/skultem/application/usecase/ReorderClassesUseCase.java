package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Clazz;
import com.moriba.skultem.domain.repository.ClassRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Re-ranks classes (e.g. JSS 1, JSS 2, JSS 3 -> 7, 8, 9). The caller sends the classes in the order they
// want; they are handed back the same set of rank numbers they already held, smallest first. That keeps
// ranks unique and leaves every class that wasn't sent (other levels, other management sections) where
// it is.
@Service
@Transactional
@RequiredArgsConstructor
public class ReorderClassesUseCase {

    private final ClassRepository classRepo;
    private final SectionScopeService sectionScopeService;

    @AuditLogAnnotation(action = "CLASSES_REORDERED")
    public void execute(String schoolId, List<String> orderedClassIds) {
        if (new HashSet<>(orderedClassIds).size() != orderedClassIds.size()) {
            throw new RuleException("A class can only appear once in the new order");
        }

        var allowedLevels = sectionScopeService.levels();
        List<Clazz> classes = new ArrayList<>();
        for (String id : orderedClassIds) {
            var clazz = classRepo.findByIdAndSchool(id, schoolId)
                    .orElseThrow(() -> new NotFoundException("Class not found"));
            if (!allowedLevels.contains(clazz.getLevel())) {
                throw new RuleException("You can't re-order '" + clazz.getName() + "'");
            }
            classes.add(clazz);
        }

        var ranks = classes.stream().map(Clazz::getDisplayOrder).sorted().toList();
        for (int i = 0; i < classes.size(); i++) {
            var clazz = classes.get(i);
            if (clazz.getDisplayOrder() != ranks.get(i)) {
                clazz.moveTo(ranks.get(i));
                classRepo.save(clazz);
            }
        }
    }
}
