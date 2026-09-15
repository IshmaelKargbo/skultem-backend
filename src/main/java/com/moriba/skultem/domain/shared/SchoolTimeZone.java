package com.moriba.skultem.domain.shared;

import java.time.ZoneId;

// Skultem only serves schools in Sierra Leone, which sits at a fixed UTC+0 year-round (no
// daylight saving) - unlike ZoneId.systemDefault(), which follows wherever the app happens to be
// deployed/run and can silently disagree with Freetown about what day, or what "today", it is.
// Use this wherever a calendar day needs to be derived from the current instant (e.g. deciding
// which day's row a clock-in/out belongs to) rather than the JVM default zone.
public final class SchoolTimeZone {

    public static final ZoneId ZONE = ZoneId.of("Africa/Freetown");

    private SchoolTimeZone() {
    }
}
