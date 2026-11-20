package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceScreening;
import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;

/** Local ClamAV INSTREAM, with a total deadline and strict bounded passive formats. */
public final class ClamAvEvidenceScreening implements PrivateEvidenceScreening {
    private final InetSocketAddress address;
    private final int timeoutMs;
    private static final Set<String> ACTIVE=Set.of("JS","JavaScript","AA","OpenAction","Launch","EmbeddedFiles","EmbeddedFile","XFA","RichMedia","URI","SubmitForm","ImportData","GoToR","GoToE");
    public ClamAvEvidenceScreening(String host,int port,int timeoutMs){
        if(!Set.of("127.0.0.1","::1").contains(host)||port<1||port>65535||timeoutMs<100||timeoutMs>10000)throw new IllegalArgumentException("EVIDENCE_SCANNER_CONFIGURATION_INVALID");
        address=new InetSocketAddress(host,port);this.timeoutMs=timeoutMs;
    }
    @Override public String inspect(byte[] bytes,String declaredMime){
        validate(bytes,declaredMime);
        long deadline=System.nanoTime()+timeoutMs*1_000_000L;
        try(var channel=SocketChannel.open();var selector=Selector.open()){
            channel.configureBlocking(false);channel.register(selector,SelectionKey.OP_CONNECT);channel.connect(address);
            while(!channel.finishConnect())await(selector,channel,SelectionKey.OP_CONNECT,deadline);
            write(channel,selector,ByteBuffer.wrap("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII)),deadline);
            for(int offset=0;offset<bytes.length;offset+=65536){int size=Math.min(65536,bytes.length-offset);write(channel,selector,ByteBuffer.allocate(4).putInt(size).flip(),deadline);write(channel,selector,ByteBuffer.wrap(bytes,offset,size),deadline);}
            write(channel,selector,ByteBuffer.allocate(4).putInt(0).flip(),deadline);
            var answer=ByteBuffer.allocate(256);boolean done=false;
            while(answer.hasRemaining()&&!done){int read=channel.read(answer);if(read<0)break;if(read==0){await(selector,channel,SelectionKey.OP_READ,deadline);continue;}for(int n=0;n<answer.position();n++)if(answer.get(n)==0){if(n!=answer.position()-1)throw new Unavailable();done=true;}}
            if(!done)throw new Unavailable();String reply=new String(answer.array(),0,answer.position()-1,StandardCharsets.US_ASCII);
            if(reply.equals("stream: OK"))return declaredMime;
            if(reply.startsWith("stream: ")&&reply.endsWith(" FOUND"))throw new Rejected();throw new Unavailable();
        }catch(Rejected|Unavailable known){throw known;}catch(IOException|RuntimeException unavailable){throw new Unavailable();}
    }
    private static void write(SocketChannel channel,Selector selector,ByteBuffer buffer,long deadline)throws IOException{while(buffer.hasRemaining()){if(System.nanoTime()>=deadline||Thread.currentThread().isInterrupted())throw new IOException();if(channel.write(buffer)==0)await(selector,channel,SelectionKey.OP_WRITE,deadline);}}
    private static void await(Selector selector,SocketChannel channel,int operation,long deadline)throws IOException{long nanos=deadline-System.nanoTime();if(nanos<=0||Thread.currentThread().isInterrupted())throw new IOException();channel.keyFor(selector).interestOps(operation);selector.select(Math.max(1,(nanos+999999)/1_000_000));selector.selectedKeys().clear();}
    private static void validate(byte[] bytes,String mime){
        if(bytes==null||bytes.length<8||bytes.length>PrivateEvidenceStore.MAX_BYTES||mime==null)throw new Rejected();
        try{
            switch(mime){
                case "image/png"->{png(bytes);image(bytes);}
                case "image/jpeg"->{if((bytes[0]&255)!=255||(bytes[1]&255)!=216||(bytes[bytes.length-2]&255)!=255||(bytes[bytes.length-1]&255)!=217)throw new Rejected();image(bytes);}
                case "application/pdf"->pdf(bytes);
                default->throw new Rejected();
            }
        }catch(Rejected rejected){throw rejected;}catch(IOException|RuntimeException invalid){throw new Rejected();}
    }
    private static void png(byte[] bytes){
        if(!Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))throw new Rejected();
        int offset=8;boolean header=false,end=false;
        while(offset<bytes.length){if(bytes.length-offset<12)throw new Rejected();int size=ByteBuffer.wrap(bytes,offset,4).getInt();if(size<0||size>bytes.length-offset-12)throw new Rejected();String type=new String(bytes,offset+4,4,StandardCharsets.US_ASCII);if(!header&&(!type.equals("IHDR")||size!=13))throw new Rejected();header=true;
            var crc=new CRC32();crc.update(bytes,offset+4,size+4);if(crc.getValue()!=Integer.toUnsignedLong(ByteBuffer.wrap(bytes,offset+8+size,4).getInt()))throw new Rejected();offset+=size+12;if(type.equals("IEND")){if(size!=0||offset!=bytes.length)throw new Rejected();end=true;break;}
        }if(!end)throw new Rejected();
    }
    private static void image(byte[] bytes)throws IOException{
        try(var stream=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new Rejected();var reader=readers.next();try{reader.setInput(stream,true,true);int width=reader.getWidth(0),height=reader.getHeight(0);if(width<1||height<1||(long)width*height>10_000_000||reader.read(0)==null)throw new Rejected();}finally{reader.dispose();}}
    }
    private static void pdf(byte[] bytes)throws IOException{
        if(!new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw new Rejected();int end=bytes.length;while(end>0&&Set.of(9,10,13,32).contains(bytes[end-1]&255))end--;if(end<5||!new String(bytes,end-5,5,StandardCharsets.US_ASCII).equals("%%EOF"))throw new Rejected();
        try(var document=Loader.loadPDF(bytes)){
            if(document.isEncrypted()||document.getNumberOfPages()<1||document.getNumberOfPages()>20)throw new Rejected();
            var seen=Collections.newSetFromMap(new IdentityHashMap<COSBase,Boolean>());var pending=new ArrayDeque<COSBase>();pending.add(document.getDocument().getTrailer());
            while(!pending.isEmpty()){COSBase item=pending.removeFirst();if(!seen.add(item))continue;if(seen.size()>50000)throw new Rejected();
                if(item instanceof COSObject object){if(object.getObject()!=null)pending.add(object.getObject());}
                else if(item instanceof COSDictionary dictionary){for(var entry:dictionary.entrySet()){if(ACTIVE.contains(entry.getKey().getName()))throw new Rejected();if(entry.getValue()!=null)pending.add(entry.getValue());}}
                else if(item instanceof COSArray array){for(COSBase entry:array)if(entry!=null)pending.add(entry);}
                else if(item instanceof COSName name&&ACTIVE.contains(name.getName()))throw new Rejected();
            }
        }
    }
}
