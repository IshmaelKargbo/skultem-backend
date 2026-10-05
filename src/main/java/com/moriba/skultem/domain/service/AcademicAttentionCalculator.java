package com.moriba.skultem.domain.service;

import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Level;
import com.moriba.skultem.domain.service.AttendanceAttentionCalculator.Trend;

// The academic counterpart of AttendanceAttentionCalculator: a term average below the pass mark is a
// concern, but where the student is heading matters just as much.
//
//   * below the pass mark and improving on last term   -> WATCH (they are already recovering)
//   * below the pass mark and steady / no earlier term -> NEEDS_ATTENTION
//   * below the pass mark and declining                -> CRITICAL
//   * still above the pass mark but slid a long way    -> WATCH (an early warning)
//
// An average needs MIN_SCORES behind it, otherwise it is "not enough data" rather than a flag.
public final class AcademicAttentionCalculator {

    public static final int MIN_SCORES = 2;
    // Points the average must move against last term to count as a trend.
    public static final double TREND_DELTA = 5.0;
    // A fall this big is worth a look even while the student is still above the pass mark.
    public static final double BIG_DROP = 15.0;

    public record Result(Double average, Double previousAverage, Trend trend, boolean belowPassMark, Level level) {
    }

    private AcademicAttentionCalculator() {
    }

    // average / scoreCount: this term. previousAverage / previousScoreCount: the term before in the
    // same academic year (null / 0 when there isn't one).
    public static Result evaluate(Double average, long scoreCount, Double previousAverage, long previousScoreCount,
            int passMark) {
        if (average == null || scoreCount < MIN_SCORES) {
            return new Result(null, null, null, false, Level.NONE);
        }

        Double previous = previousAverage != null && previousScoreCount >= MIN_SCORES ? previousAverage : null;
        Trend trend = null;
        if (previous != null) {
            double delta = average - previous;
            trend = delta >= TREND_DELTA ? Trend.IMPROVING : delta <= -TREND_DELTA ? Trend.DECLINING : Trend.STEADY;
        }

        boolean below = average < passMark;
        Level level;
        if (below) {
            level = trend == Trend.IMPROVING ? Level.WATCH
                    : trend == Trend.DECLINING ? Level.CRITICAL
                            : Level.NEEDS_ATTENTION;
        } else if (previous != null && previous - average >= BIG_DROP) {
            level = Level.WATCH;
        } else {
            level = Level.NONE;
        }
        return new Result(average, previous, trend, below, level);
    }
}
