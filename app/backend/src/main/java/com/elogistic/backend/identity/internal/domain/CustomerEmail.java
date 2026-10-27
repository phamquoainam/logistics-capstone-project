package com.elogistic.backend.identity.internal.domain;

/** Exact customer login identifier. Validation never changes an existing security contact. */
public record CustomerEmail(String value) {
    public CustomerEmail {
        if (value == null || value.length() > 254 || value.isEmpty()
                || value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)
                    || Character.isISOControl(c) || c == 0xfeff)
                || !value.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*")
                || value.substring(0, value.indexOf('@')).startsWith(".")
                || value.substring(0, value.indexOf('@')).endsWith(".")
                || value.substring(0, value.indexOf('@')).contains("..")) {
            throw new DomainViolation(DomainViolation.Reason.INVALID_STATE);
        }
    }
    @Override public String toString() { return "CustomerEmail[redacted]"; }
}
