package com.moriba.skultem.infrastructure.listener;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.moriba.skultem.domain.audit.AuditEvent;
import com.moriba.skultem.domain.model.AuditLog;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AuditLogRepository;
import com.moriba.skultem.domain.repository.UserRepository;

import ua_parser.Client;
import ua_parser.Parser;

@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);
    private static final int MAX_DETAILS_LENGTH = 2000;

    private final AuditLogRepository auditRepository;
    private final UserRepository userRepo;
    private final AcademicYearRepository academicYearRepo;
    private final Parser uaParser;

    public AuditEventListener(
            AuditLogRepository auditRepository,
            UserRepository userRepo,
            AcademicYearRepository academicYearRepo) {

        this.auditRepository  = auditRepository;
        this.userRepo         = userRepo;
        this.academicYearRepo = academicYearRepo;
        this.uaParser         = new Parser();
    }

    @Async("auditExecutor")
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        try {
            var user = event.userId() != null
                    ? userRepo.findById(event.userId()).orElse(null)
                    : null;

            var academicYear = event.schoolId() != null
                    ? academicYearRepo.findActiveBySchool(event.schoolId()).orElse(null)
                    : null;

            Client client     = uaParser.parse(event.userAgent());
            String browser    = formatBrowser(client);
            String os         = formatOs(client);
            String device     = formatDevice(client);
            String deviceType = resolveDeviceType(client);

            AuditLog auditLog = AuditLog.create(
                    UUID.randomUUID().toString(),
                    event.action(),
                    event.schoolId(),
                    academicYear,
                    user,
                    event.ip(),
                    device,
                    deviceType,
                    os,
                    browser,
                    event.status(),
                    truncate(event.details(), MAX_DETAILS_LENGTH)
            );

            auditRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to persist audit log for action '{}': {}",
                    event.action(), e.getMessage(), e);
        }
    }

    private String formatBrowser(Client client) {
        if (client.userAgent == null) return "Unknown";
        String name  = nullToEmpty(client.userAgent.family);
        String major = nullToEmpty(client.userAgent.major);
        return major.isEmpty() ? name : name + " " + major;
    }

    private String formatOs(Client client) {
        if (client.os == null) return "Unknown";
        String name  = nullToEmpty(client.os.family);
        String major = nullToEmpty(client.os.major);
        return major.isEmpty() ? name : name + " " + major;
    }

    private String formatDevice(Client client) {
        if (client.device == null) return "Unknown";
        String family = nullToEmpty(client.device.family);
        return family.isEmpty() ? "Unknown" : family;
    }

    private String resolveDeviceType(Client client) {
        if (client.device == null) return "Desktop";
        String family = nullToEmpty(client.device.family).toLowerCase();
        if (family.equals("spider") || family.equals("bot")) return "Bot";
        if (family.equals("other") || family.isEmpty())      return "Desktop";
        return "Mobile";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}