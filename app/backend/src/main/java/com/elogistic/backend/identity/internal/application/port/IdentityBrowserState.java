package com.elogistic.backend.identity.internal.application.port;
import java.util.UUID;
public interface IdentityBrowserState {
    record Challenge(String cookie,String token) { @Override public String toString(){return "CsrfChallenge[redacted]";} }
    Challenge issue(String previousCookie,String refresh);
    Challenge rebind(String previousCookie,UUID session);
    void verify(String cookie,String challenge,UUID session,boolean login);
}
