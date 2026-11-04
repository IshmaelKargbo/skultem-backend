package com.moriba.skultem.infrastructure.mail;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
public class MailService {

        private static final Logger log = LoggerFactory.getLogger(MailService.class);

        private MailtrapClient client;

        @Value("${mail.mode}")
        private String mode;

        @Value("${mailtrap.token}")
        private String token;

        @Value("${mailtrap.sender.email}")
        private String senderEmail;

        @Value("${mailtrap.sender.name}")
        private String senderName;

        @Value("${mailtrap.inbox-id}")
        private Long inboxId;

        @Value("${mailtrap.template.welcome}")
        private String welcomeTemplate;

        @Value("${mailtrap.template.parent_welcome}")
        private String parentWelcomeTemplate;

        @Value("${mailtrap.template.parent_assign}")
        private String parentAssignTemplate;

        @Value("${mailtrap.template.teacher_welcome}")
        private String teacherWelcomeTemplate;

        @Value("${mailtrap.template.user_assign}")
        private String userAssignTemplate;

        @Value("${mailtrap.template.user_welcome}")
        private String userWelcomeTemplate;

        @Value("${mailtrap.template.password_reset}")
        private String passwordResetTemplate;

        @PostConstruct
        public void init() {
                MailtrapConfig.Builder builder = new MailtrapConfig.Builder()
                                .token(token);

                if ("dev".equalsIgnoreCase(mode)) {
                        builder.sandbox(true)
                                        .inboxId(inboxId);

                        System.out.println("📦 Mail running in DEV mode (sandbox)");
                } else {
                        builder.sandbox(false);
                        System.out.println("🚀 Mail running in PRODUCTION mode");
                }

                this.client = MailtrapClientFactory
                                .createMailtrapClient(builder.build());
        }

        public void sendEmail(
                        String to,
                        String subject,
                        String html) {

                MailtrapMail mail = MailtrapMail.builder()
                                .from(new Address(senderEmail, senderName))
                                .to(List.of(new Address(to)))
                                .subject(subject)
                                .html(html)
                                .build();

                try {

                        client.send(mail);

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Email sending failed",
                                        e);
                }
        }

        public void sendTemplateEmail(
                        String to,
                        String templateUuid,
                        Map<String, Object> variables) {

                MailtrapMail mail = MailtrapMail.builder()
                                .from(new Address(senderEmail, senderName))
                                .to(List.of(new Address(to)))
                                .templateUuid(templateUuid)
                                .templateVariables(variables)
                                .build();

                try {

                        client.send(mail);

                } catch (Exception e) {

                        // Dev mode sends to the Mailtrap sandbox, where a template that can't be
                        // rendered/delivered shouldn't abort the flow that triggered the email
                        // (onboarding, user creation, ...) - just log it. Production still fails loudly.
                        if ("dev".equalsIgnoreCase(mode)) {
                                log.warn("Template email to {} (template {}) failed in dev mode: {}",
                                                to, templateUuid, e.getMessage());
                                return;
                        }

                        throw new RuntimeException(
                                        "Template email failed",
                                        e);
                }
        }

        public void sendWelcomeEmail(
                        String to,
                        String name,
                        String password,
                        String domain,
                        String link,
                        String schoolName) {

                sendTemplateEmail(
                                to,
                                welcomeTemplate,
                                Map.of(
                                                "school_domain", domain,
                                                "admin_email", to,
                                                "temporary_password", password,
                                                "login_link", link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", schoolName));
        }

        public void sendParentEmail(String to, String studentName, String className, String password,
                        String link,
                        String schoolName,
                        String domain) {

                sendTemplateEmail(to, parentWelcomeTemplate,
                                Map.of(
                                                "school_domain", domain,
                                                "student_name", studentName,
                                                "class_name", className,
                                                "temporary_password", password,
                                                "portal_link", link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", schoolName,
                                                "parent_email", to));
        }

        public void sendLinkParentEmail(String to, String studentName, String className, String parentName,
                        String link,
                        String schoolName,
                        String domain) {

                sendTemplateEmail(to, parentAssignTemplate,
                                Map.of(
                                                "school_domain", domain,
                                                "student_name", studentName,
                                                "class_name", className,
                                                "parent_name", parentName,
                                                "portal_link", link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", schoolName,
                                                "parent_email", to));
        }

        public void sendWelcomeTeacherEmail(String to, String teacherName, String password,
                        String link,
                        String schoolName,
                        String domain) {

                sendTemplateEmail(to, teacherWelcomeTemplate,
                                Map.of(
                                                "school_domain", domain,
                                                "teacher_name", teacherName,
                                                "teacher_email", to,
                                                "temporary_password", password,
                                                "login_link", link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", schoolName,
                                                "parent_email", to));
        }

        public void sendWelcomeUserEmail(WelcomeUserPayload payload) {
                sendTemplateEmail(payload.to, userWelcomeTemplate,
                                Map.of("school_domain", payload.domain,
                                                "user_name", payload.userName,
                                                "role_name", payload.role,
                                                "user_email", payload.to,
                                                "temporary_password", payload.password,
                                                "login_link", payload.link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", payload.schoolName));
        }

        public void sendAssignUserEmail(AssignUserPayload payload) {
                sendTemplateEmail(payload.to, userAssignTemplate,
                                Map.of("school_domain", payload.domain,
                                                "user_name", payload.userName,
                                                "role_name", payload.role,
                                                "login_link", payload.link,
                                                "year", String.valueOf(Year.now().getValue()),
                                                "school_name", payload.schoolName));
        }

        // "Forgot password" - the Mailtrap template takes the reset link and the year.
        public void sendPasswordResetEmail(String to, String resetLink) {
                sendTemplateEmail(to, passwordResetTemplate,
                                Map.of("reset_link", resetLink,
                                                "year", String.valueOf(Year.now().getValue())));
        }

        public record WelcomeUserPayload(String to, String userName, String password,
                        String link,
                        String schoolName,
                        String role,
                        String domain) {
        }

        public record AssignUserPayload(String to, String userName,
                        String link,
                        String schoolName,
                        String role,
                        String domain) {
        }

        // "Verify your email" - a Skultem-branded button with the link, how long it lasts, and what to do if the
        // recipient didn't ask for it. The link is never logged.
        public void sendVerificationEmail(String to, String name, String link, long expiresInMinutes, String schoolName) {
                String who = name == null || name.isBlank() ? "there" : org.springframework.web.util.HtmlUtils.htmlEscape(name);
                String from = schoolName == null || schoolName.isBlank() ? "Skultem"
                                : org.springframework.web.util.HtmlUtils.htmlEscape(schoolName) + " on Skultem";
                String href = org.springframework.web.util.HtmlUtils.htmlEscape(link);
                String html = """
                                <!DOCTYPE html>
                                <html><body style="margin:0;padding:0;background:#f4f5f7;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#1f2937;">
                                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f5f7;padding:32px 12px;">
                                  <tr><td align="center">
                                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:12px;overflow:hidden;">
                                      <tr><td style="background:#1d4ed8;padding:20px 28px;color:#ffffff;font-size:20px;font-weight:700;letter-spacing:.3px;">Skultem</td></tr>
                                      <tr><td style="padding:28px;">
                                        <p style="margin:0 0 12px;font-size:16px;">Hi %s,</p>
                                        <p style="margin:0 0 20px;font-size:15px;line-height:1.55;">Please confirm that this is your email address for %s. It lets the school reach you reliably.</p>
                                        <p style="margin:0 0 24px;"><a href="%s" style="display:inline-block;background:#1d4ed8;color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:12px 22px;border-radius:8px;">Verify email address</a></p>
                                        <p style="margin:0 0 8px;font-size:13px;color:#4b5563;">This link works once and expires in %d minutes.</p>
                                        <p style="margin:0 0 20px;font-size:13px;color:#4b5563;word-break:break-all;">If the button doesn't work, open this link:<br><a href="%s" style="color:#1d4ed8;">%s</a></p>
                                        <p style="margin:0;font-size:13px;color:#6b7280;">Didn't ask for this? You can ignore this email - nothing changes unless the link is opened.</p>
                                      </td></tr>
                                    </table>
                                    <p style="margin:16px 0 0;font-size:12px;color:#9ca3af;">Sent by Skultem school management</p>
                                  </td></tr>
                                </table>
                                </body></html>
                                """.formatted(who, from, href, expiresInMinutes, href, href);
                sendEmail(to, "Verify your email address", html);
        }
}
