package com.moriba.skultem.application.usecase;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExamTimetableDesignDTO;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The school's exam timetable PDF design (Exam Office > Timetable > Design): heading, colour, orientation, which
// details each paper shows, and the signature lines. One row per school; none = the defaults.
@Service
@Transactional
@RequiredArgsConstructor
public class ExamTimetableDesignUseCase {

    private final JdbcTemplate jdbc;

    public ExamTimetableDesignDTO get(String schoolId) {
        return jdbc.query("""
                SELECT heading, accent_color, orientation, show_logo, show_venue, show_duration, show_invigilators,
                       show_instructions, show_signatures, left_signature, right_signature, footer_note
                FROM exam_timetable_settings WHERE school_id = ?
                """, (rs, i) -> new ExamTimetableDesignDTO(rs.getString("heading"), rs.getString("accent_color"),
                rs.getString("orientation"), rs.getBoolean("show_logo"), rs.getBoolean("show_venue"),
                rs.getBoolean("show_duration"), rs.getBoolean("show_invigilators"),
                rs.getBoolean("show_instructions"), rs.getBoolean("show_signatures"),
                rs.getString("left_signature"), rs.getString("right_signature"), rs.getString("footer_note")),
                schoolId).stream().findFirst().orElseGet(ExamTimetableDesignDTO::defaults);
    }

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_DESIGN_SAVED")
    public ExamTimetableDesignDTO save(String schoolId, ExamTimetableDesignDTO in) {
        String accent = blankToNull(in.accentColor());
        if (accent != null && !accent.matches("^#[0-9a-fA-F]{6}$")) {
            throw new RuleException("The accent colour has to look like #1878c5");
        }
        String orientation = "LANDSCAPE".equals(in.orientation()) ? "LANDSCAPE" : "PORTRAIT";
        String heading = in.heading() == null || in.heading().isBlank() ? "EXAMINATION TIMETABLE"
                : in.heading().trim();
        String left = in.leftSignature() == null || in.leftSignature().isBlank() ? "Exam Officer"
                : in.leftSignature().trim();
        String right = in.rightSignature() == null || in.rightSignature().isBlank() ? "Principal"
                : in.rightSignature().trim();

        jdbc.update("""
                INSERT INTO exam_timetable_settings (school_id, heading, accent_color, orientation, show_logo,
                    show_venue, show_duration, show_invigilators, show_instructions, show_signatures,
                    left_signature, right_signature, footer_note, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (school_id) DO UPDATE SET heading = EXCLUDED.heading,
                    accent_color = EXCLUDED.accent_color, orientation = EXCLUDED.orientation,
                    show_logo = EXCLUDED.show_logo, show_venue = EXCLUDED.show_venue,
                    show_duration = EXCLUDED.show_duration, show_invigilators = EXCLUDED.show_invigilators,
                    show_instructions = EXCLUDED.show_instructions, show_signatures = EXCLUDED.show_signatures,
                    left_signature = EXCLUDED.left_signature, right_signature = EXCLUDED.right_signature,
                    footer_note = EXCLUDED.footer_note, updated_at = EXCLUDED.updated_at
                """, schoolId, heading, accent, orientation, in.showLogo(), in.showVenue(), in.showDuration(),
                in.showInvigilators(), in.showInstructions(), in.showSignatures(), left, right,
                blankToNull(in.footerNote()), Timestamp.from(Instant.now()));
        return get(schoolId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
