package com.moriba.skultem.application.mapper;

import com.moriba.skultem.domain.model.Enrollment;

// The class a student sits in, down to section and stream: "JSS 1 · A", "SSS 1 · A · Science". Report
// rows carry it so charts can split a class by section and stream instead of lumping them together.
public final class ClassSessionLabel {

    private ClassSessionLabel() {
    }

    public static String of(Enrollment enrollment) {
        if (enrollment == null || enrollment.getClazz() == null) return null;
        var label = new StringBuilder(enrollment.getClazz().getName());
        if (enrollment.getSection() != null && enrollment.getSection().getName() != null) {
            label.append(" · ").append(enrollment.getSection().getName());
        }
        var stream = enrollment.getStream();
        // "N/A" is how a missing stream shows up elsewhere in the app - treat it as none.
        if (stream != null && stream.getName() != null && !"N/A".equalsIgnoreCase(stream.getName())) {
            label.append(" · ").append(stream.getName());
        }
        return label.toString();
    }
}
