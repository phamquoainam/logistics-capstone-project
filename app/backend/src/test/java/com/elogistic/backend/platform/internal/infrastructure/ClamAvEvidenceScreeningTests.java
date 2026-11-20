package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceScreening;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClamAvEvidenceScreeningTests {
    private byte[] png()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
    @Test void realClamInstreamProtocolAndMimeValidationAreFailClosed()throws Exception{
        for(String reply:List.of("stream: OK\0","stream: Synthetic FOUND\0","stream: ERROR\0")){
            byte[] bytes=png();try(var socket=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var worker=Executors.newSingleThreadExecutor()){
                var future=worker.submit(()->{try(var s=socket.accept()){var in=new DataInputStream(s.getInputStream());assertArrayEquals("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII),in.readNBytes(10));var out=new ByteArrayOutputStream();for(int size;(size=in.readInt())!=0;){assertTrue(size>0&&size<=65536);out.write(in.readNBytes(size));}assertArrayEquals(bytes,out.toByteArray());s.getOutputStream().write(reply.getBytes(StandardCharsets.US_ASCII));return true;}catch(IOException bad){throw new UncheckedIOException(bad);}});
                var scanner=new ClamAvEvidenceScreening("127.0.0.1",socket.getLocalPort(),1000);
                if(reply.contains(" OK"))assertEquals("image/png",scanner.inspect(bytes,"image/png"));else if(reply.contains("FOUND"))assertThrows(PrivateEvidenceScreening.Rejected.class,()->scanner.inspect(bytes,"image/png"));else assertThrows(PrivateEvidenceScreening.Unavailable.class,()->scanner.inspect(bytes,"image/png"));
                assertTrue(future.get(3,TimeUnit.SECONDS));
            }
        }
    }
    @Test void mismatchedPolyglotAndActivePdfNeverReachScanner()throws Exception{
        var scanner=new ClamAvEvidenceScreening("127.0.0.1",9,100);byte[] image=png();
        assertThrows(PrivateEvidenceScreening.Rejected.class,()->scanner.inspect(image,"text/html"));
        var polyglot=Arrays.copyOf(image,image.length+6);System.arraycopy("<html>".getBytes(StandardCharsets.US_ASCII),0,polyglot,image.length,6);
        assertThrows(PrivateEvidenceScreening.Rejected.class,()->scanner.inspect(polyglot,"image/png"));
        try(var document=new PDDocument()){document.addPage(new PDPage());document.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('Synthetic')"));var out=new ByteArrayOutputStream();document.save(out);assertThrows(PrivateEvidenceScreening.Rejected.class,()->scanner.inspect(out.toByteArray(),"application/pdf"));}
        assertThrows(PrivateEvidenceScreening.Rejected.class,()->scanner.inspect(new byte[5*1024*1024+1],"image/png"));
        assertThrows(PrivateEvidenceScreening.Unavailable.class,()->scanner.inspect(image,"image/png"));
    }
}
