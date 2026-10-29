package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PublicMediaStore;
import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

/** Local sandbox store, rooted outside static web content and private evidence. */
public final class LocalPublicMediaStore implements PublicMediaStore {
    private final Path root;
    public LocalPublicMediaStore(Path root){
        this.root=Objects.requireNonNull(root).toAbsolutePath().normalize();
        if(this.root.getParent()==null)throw new IllegalArgumentException("PUBLIC_MEDIA_ROOT_INVALID");
    }
    @Override public Media read(String key){
        if(!"hero-network.svg".equals(key))throw new IllegalArgumentException("PUBLIC_MEDIA_KEY_INVALID");
        try{
            Path file=root.resolve(key);
            if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file) || Files.size(file)>262144)throw new IOException();
            return new Media(Files.readAllBytes(file),"image/svg+xml");
        }catch(IOException unavailable){throw new IllegalStateException("PUBLIC_MEDIA_UNAVAILABLE",unavailable);}
    }
}
