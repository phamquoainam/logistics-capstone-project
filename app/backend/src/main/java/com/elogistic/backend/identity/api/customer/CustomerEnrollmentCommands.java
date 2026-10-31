package com.elogistic.backend.identity.api.customer;

import java.util.UUID;

/** Customer enrollment only; no grant/organization or session is returned. */
public interface CustomerEnrollmentCommands {
    Accepted enroll(Enrollment command);
    Accepted resend(Email command);
    void verify(Verification command);
    record Enrollment(String email,String password,String displayName,String phone,String consentVersion,UUID key,UUID requestId){
        public Enrollment(String email,String password,String displayName,String consentVersion,UUID key,UUID requestId){this(email,password,displayName,null,consentVersion,key,requestId);}
        @Override public String toString(){return "CustomerEnrollmentCommand[redacted]";}
    }
    record Email(String email,UUID key,UUID requestId){@Override public String toString(){return "CustomerEmailCommand[redacted]";}}
    record Verification(UUID proofId,String proof,UUID key,UUID requestId){@Override public String toString(){return "CustomerVerification[redacted]";}}
    record Accepted(boolean accepted,UUID requestId){}
}
