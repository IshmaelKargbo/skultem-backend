package com.moriba.skultem.infrastructure.mail;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
public class MailService {

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
}