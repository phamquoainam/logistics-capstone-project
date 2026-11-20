package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.*;
import java.util.Arrays;

final class PrivateEvidenceBytes {
    private PrivateEvidenceBytes(){}
    static byte[] read(InputStream source){
        byte[] buffer=new byte[8192];
        // Wipe the accumulator's capacity as well as the returned caller-owned array later.
        class Accumulator extends ByteArrayOutputStream {void wipe(){Arrays.fill(buf,(byte)0);}}
        var output=new Accumulator();int empty=0;
        try{
            for(int read;(read=source.read(buffer,0,(int)Math.min(buffer.length,PrivateEvidenceStore.MAX_BYTES-output.size()+1)))!=-1;){
                if(Thread.currentThread().isInterrupted())throw new IOException();
                if(read==0){if(++empty>64)throw new IOException();continue;}empty=0;
                if(output.size()+read>PrivateEvidenceStore.MAX_BYTES)throw new PrivateEvidenceStore.Rejected();output.write(buffer,0,read);
            }
            if(output.size()==0)throw new PrivateEvidenceStore.Rejected();return output.toByteArray();
        }catch(PrivateEvidenceStore.Rejected rejected){throw rejected;}catch(Exception unavailable){throw new PrivateEvidenceStore.Unavailable();}
        finally{Arrays.fill(buffer,(byte)0);output.wipe();}
    }
}
