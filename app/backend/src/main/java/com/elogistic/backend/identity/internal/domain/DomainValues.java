package com.elogistic.backend.identity.internal.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import static com.elogistic.backend.identity.internal.domain.DomainViolation.Reason.*;

final class DomainValues {
    static void require(boolean valid, DomainViolation.Reason reason) {
        if (!valid) throw new DomainViolation(reason);
    }

    /** Reject lone UTF-16 surrogates rather than encoding replacement characters. */
    static int codePoints(CharSequence value, DomainViolation.Reason reason) {
        require(value != null, reason);
        int count = 0;
        for (int i = 0; i < value.length(); i++, count++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                require(i + 1 < value.length() && Character.isLowSurrogate(value.charAt(i + 1)), reason);
                i++;
            } else require(!Character.isLowSurrogate(c), reason);
        }
        return count;
    }

    static <T> Set<T> unique(Collection<T> values) {
        require(values != null, INVALID_GRANT);
        Set<T> copy = new HashSet<>();
        for (T value : values) require(value != null && copy.add(value), INVALID_GRANT);
        return Set.copyOf(copy);
    }

    private DomainValues() {}
}
