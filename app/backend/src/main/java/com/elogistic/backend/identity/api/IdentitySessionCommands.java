package com.elogistic.backend.identity.api;

import java.time.Instant;
import java.util.*;

/** Trusted backend-only commands, not HTTP DTOs. Request admission must be supplied by T-002-06. */
public interface IdentitySessionCommands {
    Result loginIdentity(Login command);
    Result refreshIdentity(Token command);
    void logoutIdentity(Token command);
    record Login(String username, String password, UUID requestId) {
        @Override public String toString() { return "IdentityLogin[redacted]"; }
    }
    final class Token implements AutoCloseable {
        private final byte[] value;
        private final UUID requestId;
        private boolean closed;
        public Token(byte[] value, UUID requestId) { this.value=value==null?null:value.clone(); this.requestId=requestId; }
        public byte[] value() { if(closed)throw new IllegalStateException("SESSION_COMMAND_CLOSED");return value==null?null:value.clone(); }
        public UUID requestId() { return requestId; }
        @Override public void close() { if(value!=null)Arrays.fill(value,(byte)0);closed=true; }
        @Override public String toString() { return "IdentitySessionToken[redacted]"; }
    }
    /** Signing facts only, NEVER an authenticated actor or a signed access JWT. T-002-06 owns signing/transport. */
    record AccessGrant(UUID sessionId, UUID accountId, long authVersion, Instant issuedAt, Instant expiresAt) {
        @Override public String toString() { return "IdentityAccessGrant[redacted]"; }
    }
    final class Result implements AutoCloseable {
        private final AccessGrant access;
        private final IdentityGrantCommands.Account user;
        private final Instant absoluteExpiresAt;
        private final byte[] refresh;
        private boolean closed;
        public Result(AccessGrant access, IdentityGrantCommands.Account user, Instant absoluteExpiresAt, byte[] refresh) {
            this.access=Objects.requireNonNull(access);this.user=Objects.requireNonNull(user);
            this.absoluteExpiresAt=Objects.requireNonNull(absoluteExpiresAt);this.refresh=refresh.clone();
        }
        public AccessGrant access() { return access; }
        public IdentityGrantCommands.Account user() { return user; }
        public Instant absoluteExpiresAt() { return absoluteExpiresAt; }
        public byte[] refreshToken() { if(closed)throw new IllegalStateException("SESSION_RESULT_CLOSED");return refresh.clone(); }
        @Override public void close() { Arrays.fill(refresh,(byte)0);closed=true; }
        @Override public String toString() { return "IdentitySessionResult[redacted]"; }
    }
}
