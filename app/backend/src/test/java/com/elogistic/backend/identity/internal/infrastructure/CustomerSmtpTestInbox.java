package com.elogistic.backend.identity.internal.infrastructure;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.*;
import static org.junit.jupiter.api.Assertions.*;

    /** Bounded synthetic inbox. Server-side peer disconnect is expected in TLS-denial cases. */
final class CustomerSmtpTestInbox implements AutoCloseable {
        final AtomicInteger authentications=new AtomicInteger(),envelopes=new AtomicInteger(),handshakes=new AtomicInteger();
        private final SSLContext serverTls;private final ServerSocket listener;private final ExecutorService executor=Executors.newSingleThreadExecutor();private final Future<?> task;
        private final List<byte[]> inbox=Collections.synchronizedList(new ArrayList<>());
        CustomerSmtpTestInbox(int connections,boolean starttls,boolean implicit,int recipientCode,SSLContext serverTls)throws Exception{
            this(connections,starttls,implicit,recipientCode,serverTls,5000);
        }
        CustomerSmtpTestInbox(int connections,boolean starttls,boolean implicit,int recipientCode,SSLContext serverTls,int acceptTimeout)throws Exception{
            if(acceptTimeout<5000 || acceptTimeout>120000)throw new IllegalArgumentException("SMTP_TEST_ACCEPT_TIMEOUT_INVALID");
            this.serverTls=serverTls;listener=implicit?serverTls.getServerSocketFactory().createServerSocket():new ServerSocket();listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0));listener.setSoTimeout(acceptTimeout);
            task=executor.submit(()->{for(int i=0;i<connections;i++)try(Socket socket=listener.accept()){socket.setSoTimeout(5000);serve(socket,starttls,implicit,recipientCode);}catch(SSLException|SocketException expectedDisconnect){}catch(Exception failure){throw new IllegalStateException("SMTP_TEST_INBOX_FAILED");}});
        }
        int port(){return listener.getLocalPort();}
        private void serve(Socket initial,boolean starttls,boolean implicit,int recipientCode)throws Exception{
            Socket socket=initial;boolean secure=implicit;
            if(implicit){((SSLSocket)socket).startHandshake();handshakes.incrementAndGet();}
            var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));var writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.US_ASCII));reply(writer,"220 test-inbox.invalid ESMTP");
            for(int commands=0;commands<40;commands++){
                String line=reader.readLine();if(line==null)return;
                if(line.startsWith("EHLO")){
                    reply(writer,"250-test-inbox.invalid");if(starttls&&!secure)reply(writer,"250-STARTTLS");if(secure)reply(writer,"250-AUTH PLAIN");reply(writer,"250 SIZE 65536");
                }else if(line.equals("STARTTLS") && starttls && !secure){
                    reply(writer,"220 Ready to start TLS");var tls=(SSLSocket)serverTls.getSocketFactory().createSocket(socket,"localhost",socket.getPort(),false);tls.setUseClientMode(false);tls.setSoTimeout(5000);tls.startHandshake();handshakes.incrementAndGet();socket=tls;secure=true;
                    reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.US_ASCII));
                }else if(line.startsWith("AUTH")){if(!secure)throw new IllegalStateException("SMTP_AUTH_BEFORE_TLS");authentications.incrementAndGet();reply(writer,"235 Authentication successful");}
                else if(line.startsWith("MAIL FROM:")){envelopes.incrementAndGet();reply(writer,"250 Sender accepted");}
                else if(line.startsWith("RCPT TO:"))reply(writer,recipientCode+" Synthetic recipient outcome");
                else if(line.equals("DATA")){
                    reply(writer,"354 Send message");var body=new ByteArrayOutputStream();
                    while((line=reader.readLine())!=null&&!line.equals(".")){if(line.startsWith(".."))line=line.substring(1);body.write(line.getBytes(StandardCharsets.US_ASCII));body.write('\r');body.write('\n');if(body.size()>65536)throw new IllegalStateException("SMTP_TEST_BODY_LIMIT");}
                    inbox.add(body.toByteArray());reply(writer,"250 Message accepted by TEST inbox");
                }else if(line.equals("QUIT")){reply(writer,"221 Closing");return;}else if(line.equals("RSET"))reply(writer,"250 Reset");else reply(writer,"500 Unsupported command");
            }
        }
        private static void reply(BufferedWriter writer,String response)throws IOException{writer.write(response);writer.write("\r\n");writer.flush();}
        List<byte[]> messages()throws Exception{task.get(10,TimeUnit.SECONDS);return List.copyOf(inbox);}
        Optional<byte[]> message(int index){synchronized(inbox){return index>=0 && index<inbox.size()?Optional.of(inbox.get(index).clone()):Optional.empty();}}
        @Override public void close()throws Exception{listener.close();executor.shutdownNow();assertTrue(executor.awaitTermination(6,TimeUnit.SECONDS));for(byte[] bytes:inbox)Arrays.fill(bytes,(byte)0);}
    }
