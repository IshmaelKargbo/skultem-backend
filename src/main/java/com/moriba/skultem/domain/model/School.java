package com.moriba.skultem.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.shared.AggregateRoot;
import com.moriba.skultem.domain.vo.Address;
import com.moriba.skultem.domain.vo.GradeBand;
import com.moriba.skultem.domain.vo.Owner;

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
    private String logo;
    private String motto;
    private String principalName;
    private String principalSignature;
    private String primaryColor;
    private String secondaryColor;
    private Double attendanceThreshold;
    private GenderComposition genderComposition;
    // System-admin-only marker for a school still being set up/tried out, not yet live. Purely a
    // flag by itself - it doesn't change any behavior on its own, but it gates
    // WipeTestSchoolDataUseCase (only a school flagged this way can have its roster/activity data
    // wiped) so that a wipe can never be pointed at a real, live school by mistake.
    private boolean testSchool;
    // How the school is managed: one scope across all its levels, or school-defined management
    // sections (see ManagementSection / SchoolLevel). Not to be confused with Section (class "A").
    private ManagementModel managementModel;

    public enum Status {
        ACTIVE,
        INACTIVE,
        DELETED
    }

    // Whether the school only enrolls one gender, or both - lets reporting/demographics (and any
    // gender-specific fee/uniform rule) know up front rather than inferring it from the roster.
    // MIXED is the default: it's the common case and the one that needs no special handling.
    public enum ManagementModel {
        UNIFIED,
        SECTION_BASED
    }

    public enum GenderComposition {
        BOYS,
        GIRLS,
        MIXED
    }

    private static final String DEFAULT_PRIMARY_COLOR = "#1878c5";
    private static final String DEFAULT_SECONDARY_COLOR = "#0f172a";
    // Below what percentage a student's attendance is flagged for attention (class "Needs
    // Attention" badge, Monthly/Term Summary, Inspection Reports). Schools set their own bar via
    // PUT /api/v1/school - 75 only applies until a school configures something else.
    private static final double DEFAULT_ATTENDANCE_THRESHOLD = 75.0;

    public School(String id, String name, String domain, Address address, Owner owner, Status status,
            List<GradeBand> gradingScale, String logo, String motto, String principalName,
            String principalSignature, String primaryColor, String secondaryColor, Double attendanceThreshold,
            GenderComposition genderComposition, boolean testSchool, ManagementModel managementModel,
            Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.name = name;
        this.address = address;
        this.status = status;
        this.owner = owner;
        this.domain = domain;
        this.gradingScale = validateAndNormalizeScale(gradingScale);
        this.logo = logo;
        this.motto = motto;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.primaryColor = primaryColor != null ? primaryColor : DEFAULT_PRIMARY_COLOR;
        this.secondaryColor = secondaryColor != null ? secondaryColor : DEFAULT_SECONDARY_COLOR;
        this.attendanceThreshold = validateThreshold(
                attendanceThreshold != null ? attendanceThreshold : DEFAULT_ATTENDANCE_THRESHOLD);
        this.genderComposition = genderComposition != null ? genderComposition : GenderComposition.MIXED;
        this.testSchool = testSchool;
        this.managementModel = managementModel != null ? managementModel : ManagementModel.UNIFIED;
        touch(updatedAt);
    }

    public static School create(String id, String name, String domain, Address address, Owner owner) {
        Instant now = Instant.now();
        return new School(id, name, domain, address, owner, Status.ACTIVE, defaultGradingScale(), null, null, null,
                null, DEFAULT_PRIMARY_COLOR, DEFAULT_SECONDARY_COLOR, DEFAULT_ATTENDANCE_THRESHOLD,
                GenderComposition.MIXED, false, ManagementModel.UNIFIED, now, now);
    }

    public void setManagementModel(ManagementModel managementModel) {
        this.managementModel = managementModel;
        touch(Instant.now());
    }

    public void markAsTest(boolean testSchool) {
        this.testSchool = testSchool;
        touch(Instant.now());
    }

    public void update(String name, String domain, Address address, Double attendanceThreshold,
            GenderComposition genderComposition) {
        this.address = address;
        this.name = name;
        this.domain = domain;
        if (attendanceThreshold != null) {
            this.attendanceThreshold = validateThreshold(attendanceThreshold);
        }
        if (genderComposition != null) {
            this.genderComposition = genderComposition;
        }
        touch(Instant.now());
    }

    private static double validateThreshold(double attendanceThreshold) {
        if (attendanceThreshold < 0 || attendanceThreshold > 100) {
            throw new RuleException("Attendance threshold must be between 0 and 100");
        }
        return attendanceThreshold;
    }

    public void updateBranding(String logo, String motto, String principalName, String principalSignature,
            String primaryColor, String secondaryColor) {
        this.logo = logo;
        this.motto = motto;
        this.principalName = principalName;
        this.principalSignature = principalSignature;
        this.primaryColor = primaryColor != null ? primaryColor : DEFAULT_PRIMARY_COLOR;
        this.secondaryColor = secondaryColor != null ? secondaryColor : DEFAULT_SECONDARY_COLOR;
        touch(Instant.now());
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
