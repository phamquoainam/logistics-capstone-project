package com.elogistic.backend.identity.internal;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.web.*;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionCustomerConfigurationTests {
    private static KeyPair system,customer;private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-27T00:00:00Z"),ZoneOffset.UTC);
    @BeforeAll static void keys()throws Exception{var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(3072);system=generator.generateKeyPair();customer=generator.generateKeyPair();}
    private static String jwk(KeyPair pair,String id,boolean secret){
        var builder=new com.nimbusds.jose.jwk.RSAKey.Builder((RSAPublicKey)pair.getPublic()).keyID(id).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256);if(secret)builder.privateKey((RSAPrivateKey)pair.getPrivate());return new JWKSet(builder.build()).toString(false);
    }
    private MockEnvironment environment(){return new MockEnvironment().withProperty("identity.security.issuer","https://identity.example.invalid").withProperty("identity.security.audience","system-browser").withProperty("identity.security.jwk-set",jwk(system,"system",true))
        .withProperty("customer.security.issuer","https://customer.example.invalid").withProperty("customer.security.audience","customer-browser").withProperty("customer.security.current-kid","customer").withProperty("customer.security.jwk-set",jwk(customer,"customer",true));}
    @Test void customerProfileCannotActivateWithoutIdentityPostgresAndSandbox(){
        for(String[] profiles:List.of(new String[]{"customer"},new String[]{"customer","postgres"},new String[]{"customer","identity"},new String[]{"customer","identity","postgres","demo"},new String[]{"customer","identity","postgres","migration"})){
            var environment=environment().withProperty("customer.rollout","SANDBOX");environment.setActiveProfiles(profiles);assertThrows(IllegalStateException.class,()->new ProductionCustomerConfiguration(environment));assertThrows(IllegalStateException.class,()->new ProductionCustomerWebConfiguration(environment));
        }
        var publicRollout=environment().withProperty("customer.rollout","PUBLIC");publicRollout.setActiveProfiles("customer","identity","postgres");assertThrows(IllegalStateException.class,()->new ProductionCustomerConfiguration(publicRollout));
        var proxy=environment().withProperty("customer.rollout","SANDBOX").withProperty("server.forward-headers-strategy","NATIVE");proxy.setActiveProfiles("customer","identity","postgres");assertThrows(IllegalStateException.class,()->new ProductionCustomerConfiguration(proxy));assertThrows(IllegalStateException.class,()->new ProductionCustomerWebConfiguration(proxy));
    }
    @Test void sandboxProfileDoesNotNeedOrGenerateCredentialsUntilExplicitRuntimeAssembly(){
        var environment=new MockEnvironment().withProperty("customer.rollout","SANDBOX");environment.setActiveProfiles("identity","postgres","customer");
        assertDoesNotThrow(()->new ProductionCustomerConfiguration(environment));assertDoesNotThrow(()->new ProductionCustomerWebConfiguration(environment));
        assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.customerTokens(environment,CLOCK));
    }
    @Test void sharedIssuerAudienceOrAnyRetainedSystemKeyCannotEnterCustomerTrustRing(){
        for(String property:List.of("customer.security.issuer","customer.security.audience","customer.security.jwk-set")){
            var environment=environment();String value=property.endsWith("issuer")?"https://identity.example.invalid":property.endsWith("audience")?"system-browser":jwk(system,"customer",true);
            environment.setProperty(property,value);var rejected=assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.customerTokens(environment,CLOCK));assertEquals("CUSTOMER_JWT_PURPOSE_CONFIGURATION_INVALID",rejected.getMessage());assertNull(rejected.getCause());
        }
    }
    @Test void missingCurrentPrivateKeyOrSystemTrustContextFailsClosed(){
        var publicOnly=environment().withProperty("customer.security.jwk-set",jwk(customer,"customer",false));assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.customerTokens(publicOnly,CLOCK));
        var missing=environment().withProperty("customer.security.current-kid","missing");assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.customerTokens(missing,CLOCK));
        var empty=environment().withProperty("identity.security.jwk-set",new JWKSet().toString());assertThrows(IllegalStateException.class,()->ProductionCustomerConfiguration.customerTokens(empty,CLOCK));
    }
    @Test void actualRs256TokensCannotCrossPurposeEvenForTheSameAccountAndSession(){
        var customerTokens=ProductionCustomerConfiguration.customerTokens(environment(),CLOCK);
        var systemTokens=new ProductionAccessTokens("https://identity.example.invalid","system-browser","system",(RSAPrivateKey)system.getPrivate(),Map.of("system",(RSAPublicKey)system.getPublic()),CLOCK);
        var grant=new IdentitySessionCommands.AccessGrant(UUID.randomUUID(),UUID.randomUUID(),1,CLOCK.instant(),CLOCK.instant().plusSeconds(900));
        String customerAccess=customerTokens.issue(grant),systemAccess=systemTokens.issue(grant);assertEquals(grant.accountId(),customerTokens.verify(customerAccess).accountId());assertEquals(grant.accountId(),systemTokens.verify(systemAccess).accountId());
        assertThrows(IdentityCommandFailure.class,()->customerTokens.verify(systemAccess));assertThrows(IdentityCommandFailure.class,()->systemTokens.verify(customerAccess));
    }
}
