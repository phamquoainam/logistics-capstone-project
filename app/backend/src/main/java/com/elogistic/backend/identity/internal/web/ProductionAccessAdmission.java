package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.internal.application.port.AccessSessionTiming;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Only the same request's verified filter may enter; IDs/client flags cannot establish admission. */
public final class ProductionAccessAdmission implements CurrentSessionAdmission {
    private record State(UUID requestId,Context context,boolean completedAction) {}
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private final AccessSessionTiming timing;
    public ProductionAccessAdmission(AccessSessionTiming timing){this.timing=Objects.requireNonNull(timing);}
    AutoCloseable enter(UUID requestId,Context verified) {
        if(requestId==null || verified==null || current.get()!=null)throw ProductionAccessTokens.invalid();
        Instant created=timing.createdAt(verified.sessionId(),verified.accountId(),verified.authVersion());
        if(created==null || verified.issuedAt().getEpochSecond()<created.getEpochSecond())throw ProductionAccessTokens.invalid();
        // JWT has integer seconds; session timestamps have PostgreSQL microseconds. Resolve ONLY
        // the same persisted session/subject/version and ONLY the identical issuing second.
        Instant issued=verified.issuedAt().getEpochSecond()==created.getEpochSecond()?created:verified.issuedAt();
        var resolved=new Context(verified.sessionId(),verified.accountId(),verified.authVersion(),issued,verified.expiresAt());
        current.set(new State(requestId,resolved,false));return ()->current.remove();
    }
    @Override public Context admit(Operation op,UUID requestId) {
        State state=current.get();
        if(state==null || !state.requestId().equals(requestId) || (op!=Operation.CURRENT_SESSION && !(op==Operation.MEANINGFUL_ACTIVITY && state.completedAction())))throw ProductionAccessTokens.invalid();
        return state.context();
    }
    void recordCompletedAction(com.elogistic.backend.identity.api.IdentityCurrentSessionCommands sessions) {
        State state=current.get();if(state==null || state.completedAction())throw ProductionAccessTokens.invalid();
        current.set(new State(state.requestId(),state.context(),true));
        try{sessions.recordMeaningfulActivity(state.requestId());}finally{current.set(state);}
    }
    UUID requestId(){State state=current.get();if(state==null)throw ProductionAccessTokens.invalid();return state.requestId();}
}
