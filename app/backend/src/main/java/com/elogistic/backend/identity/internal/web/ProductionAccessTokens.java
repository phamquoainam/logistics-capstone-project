package com.elogistic.backend.identity.internal.web;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentitySessionCommands.AccessGrant;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.security.interfaces.*;
import java.time.*;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** Production-only RS256. Explicit keys; no startup key generation, remote JWK discovery or Demo trust. */
public final class ProductionAccessTokens {
    private static final Set<String> CLAIMS=Set.of("iss","aud","sub","jti","sid","iat","nbf","exp","authVersion");
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final String issuer,audience,currentKid;
    private final RSAPrivateKey signer;
    private final Map<String,RSAPublicKey> verifiers;
    private final Clock clock;

    public ProductionAccessTokens(String issuer,String audience,String currentKid,RSAPrivateKey signer,
                                  Map<String,RSAPublicKey> verifiers,Clock clock) {
        if(issuer==null || issuer.isBlank() || issuer.length()>256 || issuer.equals("elogistics-demo")
            || audience==null || audience.isBlank() || audience.length()>256 || !id(currentKid)
            || signer==null || signer.getModulus().bitLength()<3072 || verifiers==null || verifiers.isEmpty()
            || verifiers.size()>16 || !verifiers.containsKey(currentKid) || clock==null)throw configuration();
        for(var e:verifiers.entrySet())if(!id(e.getKey()) || e.getValue()==null || e.getValue().getModulus().bitLength()<3072)throw configuration();
        if(!signer.getModulus().equals(verifiers.get(currentKid).getModulus()))throw configuration();
        this.issuer=issuer;this.audience=audience;this.currentKid=currentKid;this.signer=signer;
        this.verifiers=Map.copyOf(verifiers);this.clock=clock;
        // Validate private/public consistency, without issuing a credential or generating a key.
        try {
            var signature=java.security.Signature.getInstance("SHA256withRSA");
            byte[] probe="elogistics-production-key-configuration-v1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            signature.initSign(signer);signature.update(probe);byte[] signed=signature.sign();
            signature.initVerify(verifiers.get(currentKid));signature.update(probe);
            if(!signature.verify(signed))throw configuration();
        } catch(java.security.GeneralSecurityException invalid) {throw configuration();}
    }
    private static boolean id(String s){return s!=null && s.matches("[A-Za-z0-9_-]{1,64}");}
    private static IllegalArgumentException configuration(){return new IllegalArgumentException("IDENTITY_JWT_CONFIGURATION_INVALID");}
    static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    public String issue(AccessGrant grant) {
        try {
            if(grant==null || grant.sessionId()==null || grant.accountId()==null || grant.authVersion()<1
                || grant.issuedAt()==null || grant.expiresAt()==null)throw invalid();
            long issued=grant.issuedAt().getEpochSecond(),expires=grant.expiresAt().getEpochSecond();
            if(expires<=issued || expires-issued>900 || clock.instant().isBefore(grant.issuedAt())
                || clock.instant().getEpochSecond()>=expires)throw invalid();
            var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(grant.accountId().toString())
                .jwtID(UUID.randomUUID().toString()).claim("sid",grant.sessionId().toString()).claim("authVersion",grant.authVersion())
                .issueTime(new Date(issued*1000)).notBeforeTime(new Date(issued*1000)).expirationTime(new Date(expires*1000)).build();
            var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(currentKid).build(),claims);
            jwt.sign(new RSASSASigner(signer));return jwt.serialize();
        } catch(IdentityCommandFailure rejected){throw rejected;}
        catch(Exception rejected){throw invalid();}
    }
    public Context verify(String encoded) {
        try {
            if(encoded==null || encoded.length()>8192 || encoded.length()<100)throw invalid();
            String[] parts=encoded.split("\\.",-1);if(parts.length!=3)throw invalid();
            for(String part:parts) {
                if(!part.matches("[A-Za-z0-9_-]+"))throw invalid();
                byte[] decoded=Base64.getUrlDecoder().decode(part);
                if(!Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(part))throw invalid();
            }
            // Reject duplicate JSON members before JOSE claim coercion can hide them.
            var header=JSON.readTree(Base64.getUrlDecoder().decode(parts[0]));
            var payload=JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if(!header.isObject() || header.size()!=3 || header.get("alg")==null || header.get("typ")==null || header.get("kid")==null || !payload.isObject() || payload.size()!=CLAIMS.size())throw invalid();
            var jwt=SignedJWT.parse(encoded);var h=jwt.getHeader();
            if(!h.toJSONObject().keySet().equals(Set.of("alg","typ","kid")) || !JWSAlgorithm.RS256.equals(h.getAlgorithm())
                || !JOSEObjectType.JWT.equals(h.getType()) || !id(h.getKeyID()) || !verifiers.containsKey(h.getKeyID())
                || !jwt.verify(new RSASSAVerifier(verifiers.get(h.getKeyID()))))throw invalid();
            var c=jwt.getJWTClaimsSet();
            if(!c.getClaims().keySet().equals(CLAIMS) || !issuer.equals(c.getIssuer())
                || !payload.get("aud").isString() || !audience.equals(payload.get("aud").asString()))throw invalid();
            UUID subject=uuid(c.getSubject()),session=uuid(c.getStringClaim("sid"));uuid(c.getJWTID());
            for(String name:List.of("iat","nbf","exp","authVersion")){if(!payload.get(name).isIntegralNumber())throw invalid();new java.math.BigInteger(payload.get(name).toString()).longValueExact();}
            long issued=payload.get("iat").asLong(),nbf=payload.get("nbf").asLong(),expiry=payload.get("exp").asLong(),version=payload.get("authVersion").asLong();
            long now=clock.instant().getEpochSecond();
            if(issued<0 || nbf!=issued || expiry<=issued || expiry-issued>900 || version<1 || now<issued || now>=expiry)throw invalid();
            return new Context(session,subject,version,Instant.ofEpochSecond(issued),Instant.ofEpochSecond(expiry));
        } catch(IdentityCommandFailure rejected){throw rejected;}
        catch(Exception rejected){throw invalid();}
    }
    private static UUID uuid(String s) {UUID id=UUID.fromString(s);if(!id.toString().equals(s))throw invalid();return id;}
    @Override public String toString(){return "ProductionAccessTokens[redacted]";}
}
