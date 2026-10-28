package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

@Getter
public class Assessment extends AggregateRoot<String> {

    // Highest term number a test can be limited to.
    public static final int MAX_TERM = 6;

    private String schoolId;
    private AssessmentTemplate template;
    private String name;
    private Integer weight;
    private int position;
    // The term numbers this test runs in. Empty = every term (the usual case). A school that sits Test 1 and Test 2
    // in terms 1 and 2 but only an Exam in term 3 limits each test to its terms.
    private Set<Integer> terms;

    public Assessment(String id, String schoolId, AssessmentTemplate template, String name, Integer weight, int position,
            Instant createdAt, Instant updatedAt) {
        this(id, schoolId, template, name, weight, position, Set.of(), createdAt, updatedAt);
    }

    public Assessment(String id, String schoolId, AssessmentTemplate template, String name, Integer weight, int position,
            Set<Integer> terms, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.schoolId = schoolId;
        this.template = template;
        this.weight = validateWeight(weight);
        this.position = position;
        this.terms = validateTerms(terms);
        touch(updatedAt);
    }

    public static Assessment create(String id, String schoolId, AssessmentTemplate template, String name, Integer weight, int position) {
        return create(id, schoolId, template, name, weight, position, Set.of());
    }

    public static Assessment create(String id, String schoolId, AssessmentTemplate template, String name, Integer weight,
            int position, Set<Integer> terms) {
        Instant now = Instant.now();
        return new Assessment(id, schoolId, template, name, weight, position, terms, now, now);
    }

    public void update(String name, Integer weight, int position) {
        update(name, weight, position, this.terms);
    }

    public void update(String name, Integer weight, int position, Set<Integer> terms) {
        this.name = name;
        this.weight = validateWeight(weight);
        this.position = position;
        this.terms = validateTerms(terms);
        touch(Instant.now());
    }

    public boolean runsInTerm(int termNumber) {
        return terms.isEmpty() || terms.contains(termNumber);
    }

    // The tests that run in one term, in order.
    public static List<Assessment> forTerm(Collection<Assessment> all, int termNumber) {
        return all.stream()
                .filter(a -> a.runsInTerm(termNumber))
                .sorted(Comparator.comparingInt(Assessment::getPosition))
                .toList();
    }

    // How much of one term's mark the template's tests share.
    public static int weightInTerm(Collection<Assessment> all, int termNumber) {
        return forTerm(all, termNumber).stream().mapToInt(a -> a.getWeight() == null ? 0 : a.getWeight()).sum();
    }

    // "1,2" for storage; null when the test runs in every term.
    public static String formatTerms(Set<Integer> terms) {
        if (terms == null || terms.isEmpty()) {
            return null;
        }
        return terms.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static Set<Integer> parseTerms(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        var out = new TreeSet<Integer>();
        for (var part : stored.split(",")) {
            if (!part.isBlank()) {
                out.add(Integer.parseInt(part.trim()));
            }
        }
        return out;
    }

    private static Set<Integer> validateTerms(Set<Integer> terms) {
        if (terms == null || terms.isEmpty()) {
            return Collections.emptySortedSet();
        }
        var clean = new TreeSet<Integer>();
        for (var t : terms) {
            if (t == null || t < 1 || t > MAX_TERM) {
                throw new IllegalArgumentException("A test can only run in terms 1 to " + MAX_TERM);
            }
            clean.add(t);
        }
        return Collections.unmodifiableSortedSet(clean);
    }

    private static Integer validateWeight(Integer weight) {
        if (weight == null || weight <= 0 || weight > 100) {
            throw new IllegalArgumentException("Assessment weight must be greater than 0 and not exceed 100");
        }
        return weight;
    }
}
