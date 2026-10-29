package com.elogistic.backend.platform.api;

/** Only allowlisted, non-sensitive website media. Private evidence uses a separate contract. */
public interface PublicMediaStore {
    record Media(byte[] bytes,String contentType){
        public Media { bytes=bytes.clone(); }
        @Override public byte[] bytes(){return bytes.clone();}
    }
    Media read(String key);
}
