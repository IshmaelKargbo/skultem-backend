package com.moriba.skultem.application.services;

// Asks DNS whether a domain can receive email. Behind an interface so tests (and any future resolver) can stand in.
public interface MailDomainLookup {

    enum Result {
        // MX records, or no MX but A/AAAA records (mail then goes to the domain's own address - RFC 5321 §5.1).
        ACCEPTS_MAIL,
        // The domain exists but has nothing to deliver mail to.
        NO_MAIL_SERVER,
        // The domain doesn't exist (NXDOMAIN).
        NO_SUCH_DOMAIN,
        // The lookup itself failed (timeout, no DNS server) - says nothing about the address.
        UNKNOWN
    }

    Result lookup(String domain);
}
