package com.moriba.skultem.infrastructure.dns;

import java.util.Hashtable;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.moriba.skultem.application.services.MailDomainLookup;

import lombok.extern.slf4j.Slf4j;

// DNS through the JDK's built-in JNDI DNS provider - no extra dependency. Short timeouts so a slow resolver never
// holds up a form: anything that isn't a clear answer is UNKNOWN, which callers treat as "can't tell", not "invalid".
@Slf4j
@Component
public class JndiMailDomainLookup implements MailDomainLookup {

    private final String timeoutMs;
    private final String retries;

    public JndiMailDomainLookup(@Value("${email-validation.dns-timeout-ms:2000}") long timeoutMs,
            @Value("${email-validation.dns-retries:1}") int retries) {
        this.timeoutMs = String.valueOf(timeoutMs);
        this.retries = String.valueOf(retries);
    }

    @Override
    public Result lookup(String domain) {
        if (domain == null || domain.isBlank()) {
            return Result.NO_SUCH_DOMAIN;
        }
        InitialDirContext ctx = null;
        try {
            var env = new Hashtable<String, String>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", timeoutMs);
            env.put("com.sun.jndi.dns.timeout.retries", retries);
            ctx = new InitialDirContext(env);

            // A trailing dot makes the name absolute, so the machine's search domains aren't appended.
            String name = domain.endsWith(".") ? domain : domain + ".";
            Attributes mx = ctx.getAttributes(name, new String[] { "MX" });
            if (hasValues(mx.get("MX")) && !isNullMx(mx.get("MX"))) {
                return Result.ACCEPTS_MAIL;
            }
            if (isNullMx(mx.get("MX"))) {
                // "0 ." - the domain says outright that it accepts no mail (RFC 7505).
                return Result.NO_MAIL_SERVER;
            }
            Attributes addr = ctx.getAttributes(name, new String[] { "A", "AAAA" });
            return hasValues(addr.get("A")) || hasValues(addr.get("AAAA")) ? Result.ACCEPTS_MAIL : Result.NO_MAIL_SERVER;
        } catch (NameNotFoundException e) {
            return Result.NO_SUCH_DOMAIN;
        } catch (NamingException | RuntimeException e) {
            // Timeouts, unreachable resolvers, SERVFAIL - no conclusion about the address itself.
            log.debug("DNS lookup for {} failed: {}", domain, e.getMessage());
            return Result.UNKNOWN;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                    // nothing to do
                }
            }
        }
    }

    private static boolean hasValues(Attribute attribute) {
        return attribute != null && attribute.size() > 0;
    }

    private static boolean isNullMx(Attribute mx) {
        try {
            if (mx == null || mx.size() != 1) {
                return false;
            }
            var value = String.valueOf(mx.get(0)).trim();
            return value.equals("0 .") || value.equals("0");
        } catch (NamingException e) {
            return false;
        }
    }
}
