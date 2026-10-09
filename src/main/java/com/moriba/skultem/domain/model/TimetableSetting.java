package com.moriba.skultem.domain.model;

import java.time.Instant;

import com.moriba.skultem.domain.shared.AggregateRoot;

import lombok.Getter;

// One timetable PDF design per school, same "single active config" pattern as PayslipSetting. A null
// accentColor means "use the school's brand colour".
@Getter
public class TimetableSetting extends AggregateRoot<String> {

    private String schoolId;
    private String title;
    private String accentColor;
    private String orientation;
    private boolean showLogo;
    private boolean showIcons;
    private boolean showTeacher;
    private boolean showRoom;
    private boolean showPeriodTimes;
    private String footerNote;

    public TimetableSetting(String id, String schoolId, String title, String accentColor, String orientation,
            boolean showLogo, boolean showIcons, boolean showTeacher, boolean showRoom, boolean showPeriodTimes,
            String footerNote, Instant createdAt, Instant updatedAt) {
        super(id, createdAt);
        this.schoolId = schoolId;
        this.title = title;
        this.accentColor = accentColor;
        this.orientation = orientation;
        this.showLogo = showLogo;
        this.showIcons = showIcons;
        this.showTeacher = showTeacher;
        this.showRoom = showRoom;
        this.showPeriodTimes = showPeriodTimes;
        this.footerNote = footerNote;
        touch(updatedAt);
    }

    public static TimetableSetting create(String id, String schoolId, String title, String accentColor,
            String orientation, boolean showLogo, boolean showIcons, boolean showTeacher, boolean showRoom,
            boolean showPeriodTimes, String footerNote) {
        Instant now = Instant.now();
        return new TimetableSetting(id, schoolId, title, accentColor, orientation, showLogo, showIcons, showTeacher,
                showRoom, showPeriodTimes, footerNote, now, now);
    }

    public void update(String title, String accentColor, String orientation, boolean showLogo, boolean showIcons,
            boolean showTeacher, boolean showRoom, boolean showPeriodTimes, String footerNote) {
        this.title = title;
        this.accentColor = accentColor;
        this.orientation = orientation;
        this.showLogo = showLogo;
        this.showIcons = showIcons;
        this.showTeacher = showTeacher;
        this.showRoom = showRoom;
        this.showPeriodTimes = showPeriodTimes;
        this.footerNote = footerNote;
        touch(Instant.now());
    }
}
