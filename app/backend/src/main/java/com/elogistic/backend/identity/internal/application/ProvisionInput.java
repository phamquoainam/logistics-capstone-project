package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityProvisionCommands.Command;
import com.elogistic.backend.identity.internal.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.VALIDATION_FAILED;

/** Exact accepted request validation. Never trims/case-folds contact or identity fields. */
public record ProvisionInput(Username username, String displayName, String email, Set<Grant> grants) {
    public ProvisionInput {
        if (username == null || !validText(username.value(),1,80) || !validText(displayName,1,160) || !validText(email,1,Integer.MAX_VALUE)
                || !validEmail(email) || grants == null || grants.isEmpty()) throw invalid();
        grants = Set.copyOf(grants);
    }
    public static ProvisionInput from(Command command) {
        try {
            if (command == null || command.operationKey() == null || command.requestId() == null
                    || command.grants() == null || command.grants().isEmpty()) throw invalid();
            Set<Grant> grants=new HashSet<>();
            for (var value:command.grants()) {
                Grant grant=new Grant(new RoleCode(value.role()),new ResourceScope(ResourceScope.Type.valueOf(value.scopeType()),value.scopeId()));
                if (!grants.add(grant)) throw invalid();
            }
            return new ProvisionInput(new Username(command.username()),command.displayName(),command.email(),grants);
        } catch (IllegalArgumentException | NullPointerException rejected) { throw invalid(); }
    }
    private static boolean validText(String value,int min,int max) {
        if(value==null || !StandardCharsets.UTF_8.newEncoder().canEncode(value) || value.indexOf(0)>=0) return false;
        int length=value.codePointCount(0,value.length());return length>=min && length<=max;
    }
    /** Same full email format as ajv-formats3.0.1, with linear scans and no regex stack growth.
     * No additional address/label length limit or normalization is invented. */
    private static boolean validEmail(String value) {
        int at=value.indexOf('@');
        if(at<1 || at!=value.lastIndexOf('@') || at==value.length()-1) return false;
        boolean dot=true;
        for(int i=0;i<at;i++) {
            char c=value.charAt(i);
            if(c=='.') {if(dot)return false;dot=true;}
            else {if(!asciiAlphanumeric(c) && "!#$%&'*+/=?^_`{|}~-".indexOf(c)<0)return false;dot=false;}
        }
        if(dot)return false;
        boolean start=true,hyphen=false,domainDot=false;
        for(int i=at+1;i<value.length();i++) {
            char c=value.charAt(i);
            if(c=='.') {if(start || hyphen)return false;start=true;domainDot=true;}
            else if(c=='-') {if(start)return false;hyphen=true;}
            else {if(!asciiAlphanumeric(c))return false;start=false;hyphen=false;}
        }
        return domainDot && !start && !hyphen;
    }
    private static boolean asciiAlphanumeric(char c) {
        return (c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9');
    }
    /** Server-chosen target/correlation are not request identity; grant order is immaterial. */
    public String fingerprint() {
        try {
            var bytes=new ByteArrayOutputStream();
            try(var out=new DataOutputStream(bytes)) {
                field(out,"provisionIdentityAccount");field(out,username.value());field(out,displayName);field(out,email);
                var sorted=grants.stream().sorted(Comparator.comparing((Grant g)->g.role().value())
                    .thenComparing(g->g.scope().type().name()).thenComparing(g->g.scope().id())).toList();
                out.writeInt(sorted.size());
                for(var g:sorted) {field(out,g.role().value());field(out,g.scope().type().name());field(out,g.scope().id().toString());}
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException | java.security.NoSuchAlgorithmException impossible) {throw new IllegalStateException("Cannot fingerprint provisioning input");}
    }
    private static void field(DataOutputStream out,String value) throws IOException {
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }
    private static IdentityCommandFailure invalid() {return new IdentityCommandFailure(VALIDATION_FAILED);}
    @Override public String toString() {return "ProvisionInput[redacted]";}
}
