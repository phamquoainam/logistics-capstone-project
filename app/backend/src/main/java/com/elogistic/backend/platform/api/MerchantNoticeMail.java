package com.elogistic.backend.platform.api;

import java.util.UUID;

/** Purpose-bound transport only. The caller must own a committed, authorized outbox intent. */
public interface MerchantNoticeMail {
    Outcome send(Message message);
    enum Template { REQUEST_INFORMATION, REJECTED, APPROVED, WITHDRAWN }
    enum Kind { ACCEPTED_BY_PROVIDER, TRANSIENT_FAILURE, PERMANENT_FAILURE, BLOCKED }
    record Message(UUID id, UUID applicationId, String destination, Template template) {
        public Message {
            if(id==null||applicationId==null||id.equals(new UUID(0,0))||applicationId.equals(new UUID(0,0))
                ||destination==null||template==null)throw new IllegalArgumentException("MERCHANT_NOTICE_INVALID");
        }
        @Override public String toString(){return "MerchantNoticeMessage[redacted]";}
    }
    record Outcome(Kind kind, String reference, boolean sandbox) {
        public Outcome {
            if(kind==null||(kind==Kind.ACCEPTED_BY_PROVIDER)!=(reference!=null)
                ||reference!=null&&!reference.matches("(?:TEST|SMTP):[0-9a-f-]{36}"))throw new IllegalArgumentException("MERCHANT_NOTICE_OUTCOME_INVALID");
        }
        @Override public String toString(){return "MerchantNoticeOutcome[redacted]";}
    }
}
