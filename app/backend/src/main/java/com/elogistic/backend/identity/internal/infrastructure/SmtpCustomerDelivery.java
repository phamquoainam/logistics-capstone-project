package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.CustomerDeliveryTransport;
import com.elogistic.backend.identity.internal.domain.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.security.KeyStore;
import javax.net.ssl.*;
import java.util.*;
import java.util.logging.*;
import org.eclipse.angus.mail.smtp.*;

/** Explicit configured SMTP. No Spring auto-mail, default credentials, raw wire logging or TLS fallback. */
public final class SmtpCustomerDelivery implements CustomerDeliveryTransport {
    public enum Mode {STARTTLS,IMPLICIT_TLS,TEST_LOOPBACK}
    public record Settings(String host,int port,Mode mode,String username,String password,String sender,Set<String> approvedSenderDomains,String portalOrigin,boolean sandbox){
        public Settings{
            if(host==null || !host.matches("[A-Za-z0-9.:-]{1,253}") || port<1 || port>65535 || mode==null
                || approvedSenderDomains==null || approvedSenderDomains.isEmpty())throw configuration();
            new CustomerEmail(sender);approvedSenderDomains=Set.copyOf(approvedSenderDomains);
            String domain=sender.substring(sender.lastIndexOf('@')+1);
            if(!approvedSenderDomains.contains(domain))throw configuration();
            if(mode==Mode.TEST_LOOPBACK){if(!sandbox || !Set.of("127.0.0.1","::1").contains(host) || username!=null || password!=null)throw configuration();}
            else if(username==null || username.isBlank() || password==null || password.isEmpty())throw configuration();
            try{var uri=java.net.URI.create(portalOrigin);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
                || uri.getRawQuery()!=null || uri.getRawFragment()!=null || !(uri.getRawPath()==null || uri.getRawPath().isEmpty()))throw configuration();}
            catch(Exception invalid){throw configuration();}
        }
        @Override public String toString(){return "CustomerSmtpSettings[redacted]";}
    }
    private final Settings settings;private final Clock clock;private final SSLSocketFactory tls;
    public SmtpCustomerDelivery(Settings settings,Clock clock){this(settings,clock,null);}
    public SmtpCustomerDelivery(Settings settings,Clock clock,KeyStore approvedTrustRoots){
        this.settings=Objects.requireNonNull(settings);this.clock=Objects.requireNonNull(clock);
        try{var trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(approvedTrustRoots);
            SSLContext context=SSLContext.getInstance("TLS");context.init(null,trust.getTrustManagers(),null);tls=context.getSocketFactory();}
        catch(Exception invalid){throw configuration();}
    }
    private static IllegalArgumentException configuration(){return new IllegalArgumentException("CUSTOMER_SMTP_CONFIGURATION_INVALID");}
    private Outcome outcome(Kind kind){return new Outcome(kind,null,settings.sandbox());}
    private static boolean wireLoggingEnabled(){
        return Logger.getLogger("org.eclipse.angus.mail.smtp.protocol").isLoggable(Level.FINEST)
            || Logger.getLogger("jakarta.mail").isLoggable(Level.FINEST);
    }
    @Override public Outcome send(Message intent){
        // A process configured to trace protocol bodies cannot handle customer secrets.
        if(wireLoggingEnabled())return outcome(Kind.BLOCKED);
        Transport transport=null;byte[] token=null;
        try{
            intent.proof().requireAvailable(clock.instant());token=intent.token();
            byte[] decoded=Base64.getUrlDecoder().decode(token);
            try{if(decoded.length!=32 || !Arrays.equals(token,Base64.getUrlEncoder().withoutPadding().encode(decoded)))return outcome(Kind.BLOCKED);}
            finally{Arrays.fill(decoded,(byte)0);}
            Properties properties=new Properties();
            properties.setProperty("mail.smtp.connectiontimeout","3000");properties.setProperty("mail.smtp.timeout","3000");properties.setProperty("mail.smtp.writetimeout","3000");
            properties.setProperty("mail.smtp.ssl.checkserveridentity","true");properties.setProperty("mail.smtp.ssl.protocols","TLSv1.3 TLSv1.2");
            properties.put("mail.smtp.ssl.socketFactory",tls);properties.setProperty("mail.smtp.ssl.socketFactory.fallback","false");
            properties.setProperty("mail.smtp.auth",Boolean.toString(settings.mode()!=Mode.TEST_LOOPBACK));properties.setProperty("mail.smtp.auth.mechanisms","LOGIN PLAIN");
            properties.setProperty("mail.smtp.starttls.enable",Boolean.toString(settings.mode()==Mode.STARTTLS));properties.setProperty("mail.smtp.starttls.required",Boolean.toString(settings.mode()==Mode.STARTTLS));
            properties.setProperty("mail.smtp.ssl.enable",Boolean.toString(settings.mode()==Mode.IMPLICIT_TLS));properties.setProperty("mail.smtp.sendpartial","false");
            properties.setProperty("mail.smtp.localhost","customer-delivery.invalid");properties.setProperty("mail.debug","false");properties.setProperty("mail.debug.auth","false");properties.setProperty("mail.debug.auth.username","false");properties.setProperty("mail.debug.auth.password","false");
            Session session=Session.getInstance(properties);session.setDebug(false);session.setDebugOut(new PrintStream(OutputStream.nullOutputStream()));
            String domain=settings.sender().substring(settings.sender().lastIndexOf('@')+1),messageId="<"+intent.id()+"@"+domain+">";
            MimeMessage message=new MimeMessage(session){@Override protected void updateMessageID()throws MessagingException{setHeader("Message-ID",messageId);}};
            message.setFrom(new InternetAddress(settings.sender(),true));message.setRecipient(jakarta.mail.Message.RecipientType.TO,new InternetAddress(intent.destination(),true));
            message.setSubject("E-Logistics - Xac nhan yeu cau tai khoan","UTF-8");message.setSentDate(Date.from(clock.instant()));
            String path=switch(intent.proof().purpose()){
                case CUSTOMER_ENROLLMENT->"/verify-email";case CUSTOMER_RECOVERY->"/recover/confirm";
                case SECURITY_EMAIL_CHANGE->"/personal/security";case BUSINESS_CONTACT->"/personal/profile";case LEGACY_CUSTOMER_LINK->"/personal/security/link-customer";
                default->throw configuration();
            };
            String link=settings.portalOrigin()+path+"#proofId="+intent.proof().id()+"&proof="+new String(token,StandardCharsets.US_ASCII);
            message.setText("Yêu cầu xác nhận tài khoản E-Logistics.\nMở liên kết và chọn xác nhận; liên kết không tự kích hoạt tài khoản.\n"+link+"\nHết hạn: "+intent.proof().expiresAt()+"\nNếu bạn không thực hiện yêu cầu, hãy bỏ qua thư này.","UTF-8");message.saveChanges();
            transport=session.getTransport("smtp");transport.connect(settings.host(),settings.port(),settings.username(),settings.password());
            transport.sendMessage(message,message.getAllRecipients());
            return new Outcome(Kind.ACCEPTED_BY_PROVIDER,(settings.sandbox()?"TEST:":"SMTP:")+intent.id(),settings.sandbox());
        }catch(AuthenticationFailedException blocked){return outcome(Kind.BLOCKED);}
        catch(MessagingException failed){
            Exception cursor=failed;
            for(int depth=0;cursor!=null && depth<8;depth++){
                int code=cursor instanceof SMTPSendFailedException smtp?smtp.getReturnCode():cursor instanceof SMTPAddressFailedException address?address.getReturnCode():0;
                if(code>=500)return outcome(Kind.PERMANENT_FAILURE);if(code>=400)return outcome(Kind.TRANSIENT_FAILURE);
                if(cursor instanceof java.net.SocketTimeoutException || cursor instanceof java.net.ConnectException)return outcome(Kind.TRANSIENT_FAILURE);
                cursor=cursor instanceof MessagingException mail?mail.getNextException():cursor.getCause() instanceof Exception cause?cause:null;
            }
            return outcome(Kind.BLOCKED);
        }catch(RuntimeException blocked){return outcome(Kind.BLOCKED);}
        finally{if(token!=null)Arrays.fill(token,(byte)0);if(transport!=null)try{transport.close();}catch(MessagingException ignored){/* SMTP acceptance already has its own outcome. */}}
    }
    @Override public String toString(){return "SmtpCustomerDelivery[redacted]";}
}
