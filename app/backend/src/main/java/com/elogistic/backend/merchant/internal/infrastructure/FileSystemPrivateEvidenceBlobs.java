package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.internal.application.port.PrivateEvidenceBlobs;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.*;

/** Owner-approved SANDBOX filesystem adapter. No HTTP/static handler, scanner verdict or destructive purge. */
public final class FileSystemPrivateEvidenceBlobs implements PrivateEvidenceBlobs {
    private static final Set<PosixFilePermission> DIRECTORY=PosixFilePermissions.fromString("rwx------"),FILE=PosixFilePermissions.fromString("rw-------");
    private static final Set<AclEntryPermission> ACL=EnumSet.allOf(AclEntryPermission.class);
    private final Path root;private final UserPrincipal owner;private final boolean posix;
    private final Object rootKey;private final byte[] rootIdentity;
    private static final String IDENTITY=".private-evidence-identity";
    public FileSystemPrivateEvidenceBlobs(Path root,List<Path> publicRoots){
        try{
            if(root==null || !root.isAbsolute() || publicRoots==null || publicRoots.isEmpty())throw new IOException();
            this.root=root.normalize();if(this.root.getParent()==null)throw new IOException();
            // OS process principal, not the mutable JVM user.name property/environment display name.
            owner=this.root.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(ProcessHandle.current().info().user().orElseThrow());
            posix=this.root.getFileSystem().supportedFileAttributeViews().contains("posix");
            directories(this.root.getParent());
            Path canonicalRoot=this.root.getParent().toRealPath().resolve(this.root.getFileName());
            for(Path publicRoot:publicRoots){
                if(publicRoot==null || !publicRoot.isAbsolute())throw new IOException();
                Path blocked=Files.exists(publicRoot)?publicRoot.toRealPath():publicRoot.normalize();
                if(this.root.startsWith(publicRoot.normalize()) || canonicalRoot.startsWith(blocked))throw new IOException();
            }
            boolean created=false;
            try{if(posix)Files.createDirectory(this.root,PosixFilePermissions.asFileAttribute(DIRECTORY));else Files.createDirectory(this.root);created=true;}
            catch(FileAlreadyExistsException existing){/* Existing content is preserved and permissions are verified, never rewritten. */}
            if(created && !posix)privateAcl(this.root,true);
            directories(this.root);verifyPermissions(this.root,true);rootKey=Files.readAttributes(this.root,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS).fileKey();
            rootIdentity=initializeIdentity();
        }catch(Exception unavailable){throw new IllegalStateException("PRIVATE_EVIDENCE_STORAGE_CONFIGURATION_INVALID");}
    }
    private static void directories(Path path)throws IOException{
        for(Path current=path;current!=null;current=current.getParent()){
            var attrs=Files.readAttributes(current,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(!attrs.isDirectory() || attrs.isSymbolicLink() || attrs.isOther())throw new IOException();
        }
    }
    private void privateAcl(Path path,boolean directory)throws IOException{
        var view=Files.getFileAttributeView(path,AclFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);if(view==null || !owner.equals(view.getOwner()))throw new IOException();
        var entry=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner).setPermissions(ACL);
        if(directory)entry.setFlags(AclEntryFlag.FILE_INHERIT,AclEntryFlag.DIRECTORY_INHERIT);
        view.setAcl(List.of(entry.build()));
    }
    private void verifyPermissions(Path path,boolean directory)throws IOException{
        if(!owner.equals(Files.getOwner(path,LinkOption.NOFOLLOW_LINKS)))throw new IOException();
        if(posix){if(!Files.getPosixFilePermissions(path,LinkOption.NOFOLLOW_LINKS).equals(directory?DIRECTORY:FILE))throw new IOException();}
        else{
            var view=Files.getFileAttributeView(path,AclFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);if(view==null)throw new IOException();
            Set<AclEntryPermission> allowed=EnumSet.noneOf(AclEntryPermission.class);
            for(var entry:view.getAcl()){
                if(!owner.equals(entry.principal()) || entry.type()!=AclEntryType.ALLOW)throw new IOException();allowed.addAll(entry.permissions());
            }
            if(!allowed.containsAll(ACL))throw new IOException();
        }
    }
    private void root()throws IOException{
        directories(root);verifyPermissions(root,true);
        if(rootKey!=null && !rootKey.equals(Files.readAttributes(root,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS).fileKey()))throw new IOException();
        byte[] actual=identity();try{if(!MessageDigest.isEqual(rootIdentity,actual))throw new IOException();}finally{Arrays.fill(actual,(byte)0);}
    }
    private byte[] initializeIdentity()throws IOException{
        Path path=root.resolve(IDENTITY);byte[] fresh=new byte[32];new java.security.SecureRandom().nextBytes(fresh);
        var options=Set.<OpenOption>of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,StandardOpenOption.SYNC,LinkOption.NOFOLLOW_LINKS);
        try{
            try(var channel=posix?FileChannel.open(path,options,PosixFilePermissions.asFileAttribute(FILE)):FileChannel.open(path,options)){
                if(!posix)privateAcl(path,false);verifyPermissions(path,false);var buffer=ByteBuffer.wrap(fresh);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
            }catch(FileAlreadyExistsException existing){/* Existing private identity is loaded, never overwritten. */}
            if(posix)try(var directory=FileChannel.open(root,StandardOpenOption.READ)){directory.force(true);}
            return identity();
        }finally{Arrays.fill(fresh,(byte)0);}
    }
    private byte[] identity()throws IOException{
        Path path=root.resolve(IDENTITY);var attributes=file(path);if(attributes.size()!=32)throw new IOException();
        try(var input=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){byte[] value=input.readNBytes(33);if(value.length!=32){Arrays.fill(value,(byte)0);throw new IOException();}return value;}
    }
    private BasicFileAttributes file(Path path)throws IOException{
        var attrs=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        if(!attrs.isRegularFile() || attrs.isSymbolicLink() || attrs.isOther() || attrs.size()>MAX_BYTES)throw new IOException();
        verifyPermissions(path,false);
        if(posix && ((Number)Files.getAttribute(path,"unix:nlink",LinkOption.NOFOLLOW_LINKS)).longValue()!=1)throw new IOException();
        return attrs;
    }
    @Override public Blob put(InputStream source){
        if(source==null)throw new Rejected();Path path=null;Object createdKey=null;FileTime createdTime=null;boolean owned=false,complete=false;byte[] buffer=new byte[8192];
        try{
            root();UUID key=UUID.randomUUID();path=root.resolve(key.toString());
            var options=Set.<OpenOption>of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,StandardOpenOption.SYNC,LinkOption.NOFOLLOW_LINKS);
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;int empty=0;
            try(FileChannel channel=posix?FileChannel.open(path,options,PosixFilePermissions.asFileAttribute(FILE)):FileChannel.open(path,options)){
                owned=true;root();var created=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);createdKey=created.fileKey();createdTime=created.creationTime();
                if(!posix)privateAcl(path,false);verifyPermissions(path,false);
                int read;
                while((read=source.read(buffer,0,(int)Math.min(buffer.length,MAX_BYTES-bytes+1)))!=-1){
                    if(read==0){if(++empty>64)throw new IOException();continue;}empty=0;bytes+=read;if(bytes>MAX_BYTES)throw new Rejected();
                    digest.update(buffer,0,read);var value=ByteBuffer.wrap(buffer,0,read);while(value.hasRemaining())channel.write(value);
                }
                if(bytes==0)throw new Rejected();channel.force(true);
            }
            root();var attrs=file(path);if(attrs.size()!=bytes || !Objects.equals(createdKey,attrs.fileKey()) || !createdTime.equals(attrs.creationTime()))throw new IOException();
            if(posix)try(var directory=FileChannel.open(root,StandardOpenOption.READ)){directory.force(true);}
            Blob result=new Blob(key,bytes,HexFormat.of().formatHex(digest.digest()));complete=true;return result;
        }catch(Rejected rejected){throw rejected;}catch(Exception unavailable){throw new Unavailable();}
        finally{
            Arrays.fill(buffer,(byte)0);
            if(owned && !complete && createdTime!=null)try{
                root();var attrs=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
                if(attrs.isRegularFile() && !attrs.isSymbolicLink() && Objects.equals(createdKey,attrs.fileKey()) && createdTime.equals(attrs.creationTime()))Files.delete(path);
            }catch(IOException unavailable){/* Private owned orphan remains unavailable; no false confirmed deletion. */}
        }
    }
    @Override public Content read(Blob expected){
        if(expected==null)throw new Unavailable();byte[] bytes=null;
        try{
            root();Path path=root.resolve(expected.key().toString());var before=file(path);if(before.size()!=expected.bytes())throw new IOException();
            bytes=new byte[(int)expected.bytes()];
            try(var channel=Files.newByteChannel(path,Set.<OpenOption>of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS))){
                var buffer=ByteBuffer.wrap(bytes);int empty=0;
                while(buffer.hasRemaining()){int count=channel.read(buffer);if(count<0 || (count==0 && ++empty>64))throw new IOException();if(count>0)empty=0;}
                if(channel.read(ByteBuffer.allocate(1))!=-1)throw new IOException();
            }
            var after=file(path);root();
            if(!Objects.equals(before.fileKey(),after.fileKey()) || !before.creationTime().equals(after.creationTime()) || before.size()!=after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !MessageDigest.isEqual(HexFormat.of().parseHex(expected.sha256()),MessageDigest.getInstance("SHA-256").digest(bytes)))throw new IOException();
            return new Content(bytes);
        }catch(Exception unavailable){throw new Unavailable();}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}
    }
    @Override public String toString(){return "FileSystemPrivateEvidenceBlobs[redacted]";}
}
