package com.elogistic.backend.platform.api;

/** Returns the inspected MIME only for a clean verdict; failures must throw. Runs outside admission transactions. */
public interface PrivateEvidenceScreening {
    String inspect(byte[] bytes,String declaredMime);
    final class Rejected extends RuntimeException {public Rejected(){super("EVIDENCE_SCREENING_REJECTED");}}
    final class Unavailable extends RuntimeException {public Unavailable(){super("EVIDENCE_SCREENING_UNAVAILABLE");}}
}
