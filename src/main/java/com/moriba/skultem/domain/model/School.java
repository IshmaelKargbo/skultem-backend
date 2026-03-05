package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.vo.Address;
import com.moriba.skultem.domain.model.vo.GradeBand;
import com.moriba.skultem.domain.model.vo.Owner;
import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class School extends AggregateRoot<String> {

    private String name;
    private Address address;
    private String domain;
    private Owner owner;
    private Status status;
    private List<GradeBand> gradingScale;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    public School(String id, String name, String domain, Address address, Owner owner, Status status,
            List<GradeBand> gradingScale, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.address = address;
        this.status = status;
        this.owner = owner;
        this.domain = domain;
        this.gradingScale = validateAndNormalizeScale(gradingScale);
        touch(updatedAt);
    }

    public static School create(String id, String name, String domain, Address address, Owner owner) {
        Instant now = Instant.now();
        return new School(id, name, domain, address, owner, Status.ACTIVE, defaultGradingScale(), now, now);
    }

    public void update(String name, String domain, Address address) {
        this.address = address;
        this.name = name;
        this.domain = domain;
    }

    public void setStatus(Status status) {
        this.status = status;
        touch(Instant.now());
    }

    public void setGradingScale(List<GradeBand> gradingScale) {
        this.gradingScale = validateAndNormalizeScale(gradingScale);
        touch(Instant.now());
    }

    public String resolveGrade(int score) {
        for (GradeBand band : gradingScale) {
            if (score >= band.minScore() && score <= band.maxScore()) {
                return band.grade();
            }
        }
        return null;
    }

    private static List<GradeBand> validateAndNormalizeScale(List<GradeBand> gradingScale) {
        List<GradeBand> bands = gradingScale == null ? defaultGradingScale() : new ArrayList<>(gradingScale);

        if (bands.isEmpty()) {
            throw new RuleException("At least one grade band is required");
        }

        bands.forEach(band -> {
            if (band.grade() == null || band.grade().trim().isEmpty()) {
                throw new RuleException("Grade label is required for every band");
            }

            if (band.minScore() < 0 || band.maxScore() > 100) {
                throw new RuleException("Grade band scores must be within 0 and 100");
            }

            if (band.minScore() > band.maxScore()) {
                throw new RuleException("Each grade band min score must be less than or equal to max score");
            }
        });

        bands.sort(Comparator.comparingInt(GradeBand::maxScore).reversed());

        GradeBand first = bands.get(0);
        if (first.maxScore() != 100) {
            throw new RuleException("Highest grade band must end at 100");
        }

        GradeBand last = bands.get(bands.size() - 1);
        if (last.minScore() != 0) {
            throw new RuleException("Lowest grade band must start at 0");
        }

        for (int i = 0; i < bands.size() - 1; i++) {
            GradeBand current = bands.get(i);
            GradeBand next = bands.get(i + 1);

            if (current.minScore() != next.maxScore() + 1) {
                throw new RuleException("Grade bands must be continuous with no gaps or overlaps");
            }
        }

        return List.copyOf(bands);
    }

    private static List<GradeBand> defaultGradingScale() {
        return List.of(
                new GradeBand(80, 100, "A"),
                new GradeBand(60, 79, "B"),
                new GradeBand(50, 59, "C"),
                new GradeBand(40, 49, "D"),
                new GradeBand(0, 39, "F"));
    }
}
