package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.api.AccountView;
import java.util.List;
import java.util.Optional;

/** Module-local persistence port; credentials never cross the identity API boundary. */
public interface AccountQueries {
    record Credential(AccountView account, String passwordHash) {}
    Optional<Credential> credential(String username);
    List<AccountView> list();
}
