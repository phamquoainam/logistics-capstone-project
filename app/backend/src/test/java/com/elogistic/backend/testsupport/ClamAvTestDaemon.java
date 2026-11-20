package com.elogistic.backend.testsupport;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Controlled INSTREAM transport only, never an actual antivirus certification. */
public final class ClamAvTestDaemon implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Future<?> worker;
    private final AtomicBoolean closed=new AtomicBoolean();
    public final AtomicInteger requests=new AtomicInteger();
    public volatile String reply="stream: OK\0";
    public ClamAvTestDaemon()throws IOException{
        server=new ServerSocket(0,8,InetAddress.getByName("127.0.0.1"));
        worker=executor.submit(()->{while(!closed.get())try(var socket=server.accept()){
            socket.setSoTimeout(5000);var in=new DataInputStream(socket.getInputStream());if(!java.util.Arrays.equals(in.readNBytes(10),"zINSTREAM\0".getBytes(StandardCharsets.US_ASCII)))throw new IOException();long total=0;
            for(int bytes;(bytes=in.readInt())!=0;){if(bytes<1||bytes>65536||(total+=bytes)>5242880)throw new IOException();if(in.readNBytes(bytes).length!=bytes)throw new IOException();}
            requests.incrementAndGet();socket.getOutputStream().write(reply.getBytes(StandardCharsets.US_ASCII));
        }catch(IOException error){if(!closed.get())throw new UncheckedIOException(error);}});
    }
    public int port(){return server.getLocalPort();}
    @Override public void close()throws Exception{closed.set(true);server.close();executor.shutdown();try{worker.get(6,TimeUnit.SECONDS);}finally{executor.shutdownNow();}}
}
