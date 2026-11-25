package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.api.GovernanceFailure;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

class MerchantActivationFailuresTests {
    @Test void canonicalActivationConflictsHaveFinitePublicCodes() {
        var legacy=new DataIntegrityViolationException("redacted",new SQLException("ERROR: LEGACY_BUSINESS_KEY_CLEARANCE_REQUIRED\n  Where: canonical activation","23514"));
        var duplicate=new DataIntegrityViolationException("redacted",new SQLException("ERROR: BUSINESS_KEY_ALREADY_ACTIVATED","23505"));
        var racingDuplicate=new DataIntegrityViolationException("redacted",new SQLException("ERROR: duplicate key value violates unique constraint \"business_key_registry_pkey\"\n  Detail: redacted","23505"));
        assertEquals(GovernanceFailure.Code.LEGACY_KEY_CLEARANCE_REQUIRED,assertInstanceOf(GovernanceFailure.class,JdbcMerchantApplicationReviews.activationFailure(legacy)).code());
        assertEquals(GovernanceFailure.Code.BUSINESS_CONFLICT,assertInstanceOf(GovernanceFailure.class,JdbcMerchantApplicationReviews.activationFailure(duplicate)).code());
        assertEquals(GovernanceFailure.Code.BUSINESS_CONFLICT,assertInstanceOf(GovernanceFailure.class,JdbcMerchantApplicationReviews.activationFailure(racingDuplicate)).code());
    }
    @Test void unrelatedFailuresAndWrongSqlStatesAreNeverReclassified() {
        for(var detail:new String[][]{{"ERROR: LEGACY_BUSINESS_KEY_CLEARANCE_REQUIRED","42501"},{"ERROR: BUSINESS_KEY_ALREADY_ACTIVATED","23514"},{"ERROR: unrelated constraint","23505"},{"ERROR: actor input BUSINESS_KEY_ALREADY_ACTIVATED","23505"}}){
            var original=new DataIntegrityViolationException("redacted",new SQLException(detail[0],detail[1]));
            assertSame(original,JdbcMerchantApplicationReviews.activationFailure(original));
        }
        var unavailable=new org.springframework.dao.DataAccessResourceFailureException("redacted");
        assertSame(unavailable,JdbcMerchantApplicationReviews.activationFailure(unavailable));
    }
}
