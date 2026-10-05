package com.moriba.skultem.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Trend;

class AcademicAttentionCalculatorTest {

    private static Level level(Double avg, long n, Double prev, long pn) {
        return AcademicAttentionCalculator.evaluate(avg, n, prev, pn, 50).level();
    }

    @Test
    void tooFewScoresIsNotFlagged() {
        assertEquals(Level.NONE, level(30.0, 1, null, 0));
        assertNull(AcademicAttentionCalculator.evaluate(30.0, 1, null, 0, 50).average());
    }

    @Test
    void belowPassMarkWithNoEarlierTermNeedsAttention() {
        assertEquals(Level.NEEDS_ATTENTION, level(40.0, 5, null, 0));
    }

    @Test
    void belowPassMarkAndImprovingIsOnlyWatch() {
        assertEquals(Level.WATCH, level(45.0, 5, 30.0, 5));
    }

    @Test
    void belowPassMarkAndDecliningIsCritical() {
        var r = AcademicAttentionCalculator.evaluate(40.0, 5, 60.0, 5, 50);
        assertEquals(Trend.DECLINING, r.trend());
        assertEquals(Level.CRITICAL, r.level());
    }

    @Test
    void bigDropWhileStillPassingIsWatch() {
        assertEquals(Level.WATCH, level(60.0, 5, 80.0, 5));
        assertEquals(Level.NONE, level(70.0, 5, 75.0, 5));
    }

    @Test
    void earlierTermWithTooFewScoresIsIgnored() {
        assertEquals(Level.NEEDS_ATTENTION, level(40.0, 5, 90.0, 1));
    }
}
