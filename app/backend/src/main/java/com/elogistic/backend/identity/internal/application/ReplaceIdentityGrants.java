package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityGrantCommands;
import com.elogistic.backend.identity.internal.application.port.GrantAuthority;
import com.elogistic.backend.identity.internal.application.port.IdentityGrantStore;
import com.elogistic.backend.identity.internal.domain.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Explicitly composed production use case; no Demo component scan/HTTP wiring. */
public final class ReplaceIdentityGrants implements IdentityGrantCommands {
    private final IdentityGrantStore store;
    private final GrantAuthority authorities;
    private final Clock clock;
    private final Duration receiptRetention;

    public ReplaceIdentityGrants(IdentityGrantStore store, GrantAuthority authorities,
                                Clock clock, Duration receiptRetention) {
        this.store = Objects.requireNonNull(store);
        this.authorities = authorities;
        this.clock = Objects.requireNonNull(clock);
        if (receiptRetention == null || receiptRetention.compareTo(Duration.ofHours(24)) < 0)
            throw new IllegalArgumentException("Receipt retention must cover accepted proof lifetimes");
        this.receiptRetention = receiptRetention;
    }

    @Override public Result replaceIdentityGrants(Command command) {
        if (command == null || command.sessionId() == null) throw failure(SESSION_INVALID);
        return store.transaction(tx -> {
            tx.lockAdministration();
            UUID actorId = tx.sessionAccount(command.sessionId()).orElseThrow(() -> failure(SESSION_INVALID));
            IdentityAccount actor = tx.lockAccount(actorId).orElseThrow(() -> failure(SESSION_INVALID));
            IdentitySession session;
            try {
                session = tx.lockSession(command.sessionId()).orElseThrow(() -> failure(SESSION_INVALID));
                session.admit(actor, clock.instant());
            }
            catch (DomainViolation denied) { throw failure(SESSION_INVALID); }
            if (command.targetId() == null) throw failure(ACCOUNT_NOT_ACCESSIBLE);
            IdentityAccount target = tx.lockAccount(command.targetId())
                .orElseThrow(() -> failure(ACCOUNT_NOT_ACCESSIBLE));
            Set<Grant> proposed = grants(command);
            ManagementAuthority authority = authorities == null ? null : authorities.resolve(actor, target, proposed);
            // Same Domain guard as mutation, without incrementing state or evaluating stale
            // mutation-only preconditions before an authorized receipt replay.
            if (authority == null) throw failure(ACCOUNT_NOT_ACCESSIBLE);
            try { authority.requireManagement(actor, target, proposed); }
            catch (DomainViolation denied) { throw mapped(denied); }
            if (command.operationKey() == null || command.requestId() == null || command.expectedVersion() < 1
                    || command.reason() == null || command.reason().codePointCount(0, command.reason().length()) < 1
                    || command.reason().codePointCount(0, command.reason().length()) > 300
                    || !StandardCharsets.UTF_8.newEncoder().canEncode(command.reason()))
                throw failure(VALIDATION_FAILED);
            String fingerprint = fingerprint(command, proposed);
            var now = clock.instant(); // recheck deadlines after target/owner resolution, never a request time
            try { session.admit(actor, now); }
            catch (DomainViolation denied) { throw failure(SESSION_INVALID); }
            var old = tx.receipt(actor.id(), command.operationKey());
            if (old.isPresent()) {
                var receipt = old.get();
                if (!receipt.targetId().equals(target.id()) || !receipt.fingerprint().equals(fingerprint)
                        || receipt.originalVersion() != command.expectedVersion()) throw failure(IDEMPOTENCY_CONFLICT);
                return new Result(tx.accountView(target.id()), receipt.resultVersion(), true);
            }
            IdentityAccount changed;
            try { changed = target.replaceGrants(actor, authority, proposed, command.expectedVersion(), tx.activeAdministrators()); }
            catch (DomainViolation denied) { throw mapped(denied); }
            tx.replace(target, changed, now);
            tx.revokeSessions(target.id(), now);
            tx.appendAudit(actor.id(), target.id(), command.requestId(), command.reason(), now);
            tx.appendReceipt(actor.id(), command.operationKey(), new IdentityGrantStore.Receipt(
                target.id(), command.expectedVersion(), changed.version().version(), fingerprint),
                now, now.plus(receiptRetention));
            return new Result(tx.accountView(target.id()), changed.version().version(), false);
        });
    }

    private Set<Grant> grants(Command command) {
        try {
            if (command.grants() == null || command.grants().isEmpty()) throw failure(VALIDATION_FAILED);
            Set<Grant> result = new HashSet<>();
            for (GrantData input : command.grants()) {
                Grant grant = new Grant(new RoleCode(input.role()),
                    new ResourceScope(ResourceScope.Type.valueOf(input.scopeType()), input.scopeId()));
                if (!result.add(grant)) throw failure(VALIDATION_FAILED);
            }
            return Set.copyOf(result);
        } catch (IllegalArgumentException | NullPointerException invalid) { throw failure(VALIDATION_FAILED); }
    }

    private String fingerprint(Command command, Set<Grant> grants) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var data = new DataOutputStream(bytes)) {
                field(data, "replaceIdentityGrants"); field(data, command.targetId().toString());
                data.writeLong(command.expectedVersion()); field(data, command.reason());
                var ordered = grants.stream().sorted(Comparator.comparing((Grant g) -> g.role().value())
                    .thenComparing(g -> g.scope().type().name()).thenComparing(g -> g.scope().id())).toList();
                data.writeInt(ordered.size());
                for (Grant g : ordered) { field(data, g.role().value()); field(data, g.scope().type().name()); field(data, g.scope().id().toString()); }
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Cannot fingerprint Identity command");
        }
    }
    private void field(DataOutputStream data, String value) throws java.io.IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8); data.writeInt(bytes.length); data.write(bytes);
    }
    private IdentityCommandFailure mapped(DomainViolation e) {
        return failure(switch (e.reason()) {
            case SELF_MANAGEMENT -> SELF_MANAGEMENT_FORBIDDEN;
            case LAST_ACTIVE_ADMIN -> LAST_ADMIN_PROTECTED;
            case VERSION_CONFLICT -> PRECONDITION_STALE;
            case AUTHORITY_REQUIRED -> ACCOUNT_NOT_ACCESSIBLE;
            default -> VALIDATION_FAILED;
        });
    }
    private IdentityCommandFailure failure(IdentityCommandFailure.Code code) { return new IdentityCommandFailure(code); }
}
