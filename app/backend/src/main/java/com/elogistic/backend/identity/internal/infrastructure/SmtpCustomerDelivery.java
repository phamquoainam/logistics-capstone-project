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
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Explicit configured JavaMailSender transport. No default credentials, raw wire logging or TLS fallback. */
public final class SmtpCustomerDelivery implements CustomerDeliveryTransport {
    public enum Mode {STARTTLS,IMPLICIT_TLS,TEST_LOOPBACK,LOCAL_REAL_SMTP}
    public record Settings(String host,int port,Mode mode,String username,String password,String sender,Set<String> approvedSenderDomains,String portalOrigin,boolean sandbox){
        public Settings{
            if(host==null || !host.matches("[A-Za-z0-9.:-]{1,253}") || port<1 || port>65535 || mode==null
                || approvedSenderDomains==null || approvedSenderDomains.isEmpty())throw configuration();
            new CustomerEmail(sender);approvedSenderDomains=Set.copyOf(approvedSenderDomains);
            String domain=sender.substring(sender.lastIndexOf('@')+1);
            if(!approvedSenderDomains.contains(domain))throw configuration();
            if(mode==Mode.TEST_LOOPBACK){if(!sandbox || !Set.of("127.0.0.1","::1").contains(host) || username!=null || password!=null)throw configuration();}
            else if(username==null || username.isBlank() || password==null || password.isEmpty())throw configuration();
            if(mode==Mode.LOCAL_REAL_SMTP && (!sandbox || !"smtp.gmail.com".equals(host) || port!=587 || !username.equalsIgnoreCase(sender)))throw configuration();
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
    private boolean testTransport(){return settings.mode()==Mode.TEST_LOOPBACK;}
    private Outcome outcome(Kind kind){return new Outcome(kind,null,testTransport());}
    private static boolean wireLoggingEnabled(){
        return Logger.getLogger("org.eclipse.angus.mail.smtp.protocol").isLoggable(Level.FINEST)
            || Logger.getLogger("jakarta.mail").isLoggable(Level.FINEST);
    }
    private static Kind failureKind(Throwable failure){
        var pending=new ArrayDeque<Throwable>();var seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());pending.add(failure);
        for(int examined=0;!pending.isEmpty() && examined<32;examined++){
            Throwable current=pending.removeFirst();if(!seen.add(current))continue;
            int code=current instanceof SMTPSendFailedException smtp?smtp.getReturnCode():current instanceof SMTPAddressFailedException address?address.getReturnCode():0;
            if(code>=500)return Kind.PERMANENT_FAILURE;if(code>=400)return Kind.TRANSIENT_FAILURE;
            if(current instanceof java.net.SocketTimeoutException || current instanceof java.net.ConnectException)return Kind.TRANSIENT_FAILURE;
            if(current instanceof MailSendException mail)for(Exception nested:mail.getMessageExceptions())if(nested!=null)pending.add(nested);
            if(current instanceof MessagingException mail && mail.getNextException()!=null)pending.add(mail.getNextException());
            if(current.getCause()!=null)pending.add(current.getCause());
        }
        return Kind.BLOCKED;
    }
    @Override public Outcome send(Message intent){
        // A process configured to trace protocol bodies cannot handle customer secrets.
        if(wireLoggingEnabled())return outcome(Kind.BLOCKED);
        byte[] token=null;
        try{
            intent.proof().requireAvailable(clock.instant());token=intent.token();
            if(intent.proof().emailOtp()){
                if(token.length!=43)return outcome(Kind.BLOCKED);
                for(int index=0;index<6;index++)if(token[index]<'0' || token[index]>'9')return outcome(Kind.BLOCKED);
            }else{
                byte[] decoded=Base64.getUrlDecoder().decode(token);
                try{if(decoded.length!=32 || !Arrays.equals(token,Base64.getUrlEncoder().withoutPadding().encode(decoded)))return outcome(Kind.BLOCKED);}
                finally{Arrays.fill(decoded,(byte)0);}
            }
            Properties properties=new Properties();
            properties.setProperty("mail.smtp.connectiontimeout","3000");properties.setProperty("mail.smtp.timeout","3000");properties.setProperty("mail.smtp.writetimeout","3000");
            properties.setProperty("mail.smtp.ssl.checkserveridentity","true");properties.setProperty("mail.smtp.ssl.protocols","TLSv1.3 TLSv1.2");
            properties.put("mail.smtp.ssl.socketFactory",tls);properties.setProperty("mail.smtp.ssl.socketFactory.fallback","false");
            properties.setProperty("mail.smtp.auth",Boolean.toString(settings.mode()!=Mode.TEST_LOOPBACK));properties.setProperty("mail.smtp.auth.mechanisms","LOGIN PLAIN");
            boolean starttls=settings.mode()==Mode.STARTTLS || settings.mode()==Mode.LOCAL_REAL_SMTP;
            properties.setProperty("mail.smtp.starttls.enable",Boolean.toString(starttls));properties.setProperty("mail.smtp.starttls.required",Boolean.toString(starttls));
            properties.setProperty("mail.smtp.ssl.enable",Boolean.toString(settings.mode()==Mode.IMPLICIT_TLS));properties.setProperty("mail.smtp.sendpartial","false");
            properties.setProperty("mail.smtp.localhost","customer-delivery.invalid");properties.setProperty("mail.debug","false");properties.setProperty("mail.debug.auth","false");properties.setProperty("mail.debug.auth.username","false");properties.setProperty("mail.debug.auth.password","false");
            var sender=new JavaMailSenderImpl();sender.setHost(settings.host());sender.setPort(settings.port());
            if(settings.mode()!=Mode.TEST_LOOPBACK){sender.setUsername(settings.username());sender.setPassword(settings.password());}
            sender.setJavaMailProperties(properties);
            Session session=sender.getSession();session.setDebug(false);session.setDebugOut(new PrintStream(OutputStream.nullOutputStream()));
            String domain=settings.sender().substring(settings.sender().lastIndexOf('@')+1),messageId="<"+intent.id()+"@"+domain+">";
            MimeMessage message=new MimeMessage(session){@Override protected void updateMessageID()throws MessagingException{setHeader("Message-ID",messageId);}};
            message.setFrom(new InternetAddress(settings.sender(),true));message.setRecipient(jakarta.mail.Message.RecipientType.TO,new InternetAddress(intent.destination(),true));
            boolean recovery=intent.proof().purpose()==CustomerProof.Purpose.CUSTOMER_RECOVERY;
            message.setSubject(recovery?"movapost. - Xác minh khôi phục mật khẩu":"movapost. - Xác minh địa chỉ email","UTF-8");message.setSentDate(Date.from(clock.instant()));
            String path=switch(intent.proof().purpose()){
                case CUSTOMER_ENROLLMENT->"/verify-email";case CUSTOMER_RECOVERY->"/recover/confirm";
                case SECURITY_EMAIL_CHANGE->"/personal/security";case BUSINESS_CONTACT->"/personal/profile";case LEGACY_CUSTOMER_LINK->"/personal/security/link-customer";
                default->throw configuration();
            };
            String link=settings.portalOrigin()+path+"#proofId="+intent.proof().id()
                +(intent.proof().emailOtp()?"&expiresAt="+intent.proof().expiresAt():"&proof="+new String(token,StandardCharsets.US_ASCII));
            boolean otp=intent.proof().emailOtp();
            String code=otp?new String(token,0,6,StandardCharsets.US_ASCII):"";
            String title=recovery?"Xác minh khôi phục mật khẩu":"Xác minh địa chỉ email của bạn";
            String instruction=recovery?"Vui lòng nhập mã xác minh sau vào màn hình khôi phục để đặt mật khẩu mới.":"Vui lòng nhập mã xác minh sau vào màn hình đăng ký để kích hoạt tài khoản.";
            String content=otp
                ?"movapost.\n"+title+"\n\n"+(recovery?"":"Chào mừng bạn đến với movapost.\n\n")+instruction+"\n\n"+code+"\nMã có hiệu lực trong 2 phút.\n\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.\n\nmovapost. — Gửi niềm tin, nối hành trình."
                :"movapost.\n"+title+"\n\nMở liên kết sau để tiếp tục:\n"+link+"\nNếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.";
            if(otp){
                var plain=new MimeBodyPart();plain.setText(content,"UTF-8");
                var html=new MimeBodyPart();html.setContent("<!doctype html><html lang=\"vi\"><body style=\"margin:0;background:#f4f6fa;font-family:Arial,sans-serif;color:#192d4b\"><table role=\"presentation\" style=\"max-width:560px;width:100%;margin:32px auto;background:#fff;border-radius:12px\"><tr><td style=\"padding:32px\"><div style=\"font-size:28px;font-weight:800;letter-spacing:-1px\"><span style=\"color:#1955a6\">mova</span><span style=\"color:#ef751b\">post.</span></div><h1 style=\"font-size:21px;margin:28px 0 16px\">"+title+"</h1>"+(recovery?"":"<p>Chào mừng bạn đến với movapost.</p>")+"<p style=\"line-height:1.7\">"+instruction+"</p><div style=\"background:#fff5eb;border-radius:8px;text-align:center;padding:22px;margin:24px 0\"><strong style=\"font-size:34px;letter-spacing:8px;color:#192d4b\">"+code+"</strong><p style=\"font-size:13px;color:#67748a;margin:12px 0 0\">Có hiệu lực trong 2 phút</p></div><p style=\"font-size:14px;line-height:1.7\">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email.</p><p style=\"border-top:1px solid #e9edf3;padding-top:22px;font-size:12px;color:#67748a\">movapost. — Gửi niềm tin, nối hành trình.</p></td></tr></table></body></html>","text/html; charset=UTF-8");
                var alternatives=new MimeMultipart("alternative");alternatives.addBodyPart(plain);alternatives.addBodyPart(html);message.setContent(alternatives);
            }else message.setText(content,"UTF-8");
            message.saveChanges();
            sender.send(message);
            return new Outcome(Kind.ACCEPTED_BY_PROVIDER,(testTransport()?"TEST:":"SMTP:")+intent.id(),testTransport());
        }catch(MailAuthenticationException blocked){return outcome(Kind.BLOCKED);}
        catch(MailException failed){return outcome(failureKind(failed));}
        catch(MessagingException | RuntimeException blocked){return outcome(Kind.BLOCKED);}
        finally{if(token!=null)Arrays.fill(token,(byte)0);}
    }
    @Override public String toString(){return "SmtpCustomerDelivery[redacted]";}
}
