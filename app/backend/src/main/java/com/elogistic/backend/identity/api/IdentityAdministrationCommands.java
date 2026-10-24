package com.elogistic.backend.identity.api;

import java.util.List;
import java.util.UUID;

/** Backend Application boundary only; trusted admission supplies actor/session identity. */
public interface IdentityAdministrationCommands {
    AccountPage listIdentityAccounts(Query query);
    IdentityGrantCommands.Account getIdentityAccount(UUID targetId, UUID requestId);
    RoleCatalog listIdentityRoles(UUID requestId);
    ScopeCatalog listIdentityScopes(UUID requestId);
    IdentityGrantCommands.Result disableIdentityAccount(Command command);
    IdentityGrantCommands.Result reactivateIdentityAccount(Command command);
    void resendIdentityInvitation(Command command);

    record Query(UUID requestId, String cursor, Integer limit) {}
    record Command(UUID requestId, UUID targetId, long expectedVersion, UUID operationKey, String reason) {
        @Override public String toString() { return "IdentityAdministrationCommand[redacted]"; }
    }
    record AccountPage(List<IdentityGrantCommands.Account> items, String nextCursor) {
        public AccountPage { items=List.copyOf(items); }
        @Override public String toString() { return "IdentityAccountPage[redacted]"; }
    }
    record Role(String code, String description) {}
    record Scope(UUID id, String type, String displayName) {}
    record RoleCatalog(List<Role> items) { public RoleCatalog { items=List.copyOf(items); } }
    record ScopeCatalog(List<Scope> items) { public ScopeCatalog { items=List.copyOf(items); } }
}
