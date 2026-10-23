package com.elogistic.backend.identity.api;

import java.util.Arrays;
import java.util.UUID;

/** Backend-only invitation application boundary. The proof ID is a locator, NEVER authority.
 * The future HTTP/security adapter must supply verified request admission separately;
 * this is not a new HTTP field, public endpoint, token envelope or automatic login.
 * Caller owns the command and must close it, including on failure. */
public interface IdentityInvitationCommands {
    void acceptIdentityInvitation(Command command);

    final class Command implements AutoCloseable {
        private final UUID proofId, operationKey, requestId;
        private final String password;
        private final byte[] token;
        private boolean closed;
        public Command(UUID proofId, byte[] token, String password, UUID operationKey, UUID requestId) {
            this.proofId=proofId; this.token=token==null ? null : token.clone();
            this.password=password; this.operationKey=operationKey; this.requestId=requestId;
        }
        public UUID proofId(){return proofId;}
        public UUID operationKey(){return operationKey;}
        public UUID requestId(){return requestId;}
        public String password(){if(closed)throw new IllegalStateException("INVITATION_COMMAND_CLOSED");return password;}
        public byte[] token(){if(closed)throw new IllegalStateException("INVITATION_COMMAND_CLOSED");return token==null ? null : token.clone();}
        @Override public void close(){if(token!=null)Arrays.fill(token,(byte)0);closed=true;}
        @Override public String toString(){return "IdentityInvitationCommand[redacted]";}
    }
}
