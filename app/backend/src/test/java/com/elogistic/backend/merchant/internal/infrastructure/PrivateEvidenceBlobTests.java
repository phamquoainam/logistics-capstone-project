package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.merchant.internal.application.port.PrivateEvidenceBlobs;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PrivateEvidenceBlobTests {
    @TempDir Path temporary;
    private FileSystemPrivateEvidenceBlobs store;private Path root;
    private static final byte[] DATA="Synthetic private evidence bytes".getBytes(StandardCharsets.US_ASCII);
    @BeforeEach void setup(){root=temporary.resolve("private-evidence");store=new FileSystemPrivateEvidenceBlobs(root,List.of(temporary.resolve("public")));}
    private PrivateEvidenceBlobs.Blob put(){return store.put(new ByteArrayInputStream(DATA));}
    private long count()throws IOException{try(var files=Files.list(root)){return files.filter(path->!path.getFileName().toString().equals(".private-evidence-identity")).count();}}
    @Test void privateQuarantineRoundTripHasServerKeyExactDigestAndDefensiveContent()throws Exception{
        var blob=put();assertEquals(DATA.length,blob.bytes());assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(DATA)),blob.sha256());assertEquals(1,count());assertTrue(Files.isRegularFile(root.resolve(blob.key().toString()),LinkOption.NOFOLLOW_LINKS));
        try(var content=store.read(blob)){byte[] copy=content.bytes();assertArrayEquals(DATA,copy);copy[0]=0;assertArrayEquals(DATA,content.bytes());assertFalse(content.toString().contains("Synthetic"));}assertFalse(store.toString().contains(root.toString()));
    }
    @Test void fiveMiBBoundaryIsExactAndOversizeOrEmptyNeverLeavesPartialBlob()throws Exception{
        byte[] maximum=new byte[(int)PrivateEvidenceBlobs.MAX_BYTES];Arrays.fill(maximum,(byte)7);var blob=store.put(new ByteArrayInputStream(maximum));assertEquals(maximum.length,blob.bytes());try(var content=store.read(blob)){assertArrayEquals(maximum,content.bytes());}
        assertThrows(PrivateEvidenceBlobs.Rejected.class,()->store.put(new ByteArrayInputStream(new byte[maximum.length+1])));assertThrows(PrivateEvidenceBlobs.Rejected.class,()->store.put(new ByteArrayInputStream(new byte[0])));assertEquals(1,count());
    }
    @Test void failedOrNonProgressingInputCleansOnlyItsOwnPartialFile()throws Exception{
        var original=put();InputStream broken=new InputStream(){int reads;public int read()throws IOException{if(reads++>2)throw new IOException("sensitive-source-must-not-escape");return 1;}};
        var error=assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.put(broken));assertNull(error.getCause());assertEquals("PRIVATE_EVIDENCE_UNAVAILABLE",error.getMessage());assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.put(new InputStream(){public int read(){return 0;}public int read(byte[] b,int offset,int size){return 0;}}));assertEquals(1,count());try(var content=store.read(original)){assertArrayEquals(DATA,content.bytes());}
    }
    @Test void wrongDigestLengthMissingOrChangedBytesAlwaysDenyRead()throws Exception{
        var blob=put();for(var expected:List.of(new PrivateEvidenceBlobs.Blob(blob.key(),blob.bytes(),"0".repeat(64)),new PrivateEvidenceBlobs.Blob(blob.key(),blob.bytes()+1,blob.sha256()),new PrivateEvidenceBlobs.Blob(UUID.randomUUID(),blob.bytes(),blob.sha256())))assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(expected));
        byte[] changed=DATA.clone();changed[0]^=1;Files.write(root.resolve(blob.key().toString()),changed,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));
    }
    @Test void replacementGetsFreshRandomKeyAndNeverOverwritesPriorBytes()throws Exception{
        var before=put();var after=put();assertNotEquals(before.key(),after.key());assertEquals(2,count());try(var content=store.read(before)){assertArrayEquals(DATA,content.bytes());}try(var content=store.read(after)){assertArrayEquals(DATA,content.bytes());}
    }
    @Test void publicRelativeOrMissingParentRootCannotBeConfigured()throws Exception{
        var publicRoot=Files.createDirectory(temporary.resolve("public-root"));
        for(Path invalid:List.of(Path.of("relative-root"),publicRoot,publicRoot.resolve("private"),temporary.resolve("missing-parent").resolve("root")))assertEquals("PRIVATE_EVIDENCE_STORAGE_CONFIGURATION_INVALID",assertThrows(IllegalStateException.class,()->new FileSystemPrivateEvidenceBlobs(invalid,List.of(publicRoot)),invalid.toString()).getMessage());
        assertThrows(IllegalStateException.class,()->new FileSystemPrivateEvidenceBlobs(root,List.of()));assertThrows(IllegalStateException.class,()->new FileSystemPrivateEvidenceBlobs(root,List.of(Path.of("relative-public"))));assertEquals(0,count());
    }
    @Test void insecureExistingPermissionsAreRejectedWithoutRepairOrContentLoss()throws Exception{
        var blob=put();var posix=Files.getFileAttributeView(root,PosixFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
        if(posix!=null){var original=posix.readAttributes().permissions();try{posix.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x"));deniedExisting(blob);}finally{posix.setPermissions(original);}}
        else{var acl=Files.getFileAttributeView(root,AclFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);assertNotNull(acl);var original=acl.getAcl();var changed=new ArrayList<>(original);changed.add(AclEntry.newBuilder().setType(AclEntryType.DENY).setPrincipal(acl.getOwner()).setPermissions(AclEntryPermission.WRITE_DATA).build());try{acl.setAcl(changed);deniedExisting(blob);assertEquals(changed,acl.getAcl());}finally{acl.setAcl(original);}}
        try(var content=store.read(blob)){assertArrayEquals(DATA,content.bytes());}
    }
    private void deniedExisting(PrivateEvidenceBlobs.Blob blob){assertThrows(IllegalStateException.class,()->new FileSystemPrivateEvidenceBlobs(root,List.of(temporary.resolve("public"))));assertThrows(PrivateEvidenceBlobs.Unavailable.class,this::put);assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));}
    @Test void nonRegularObjectsAndPosixLinksCannotBeReadAsEvidence()throws Exception{
        var blob=put();Path path=root.resolve(blob.key().toString());Files.delete(path);Files.createDirectory(path);assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));Files.delete(path);
        if(Files.getFileAttributeView(root,PosixFileAttributeView.class)!=null){Path foreign=temporary.resolve("foreign");Files.write(foreign,DATA);Files.setPosixFilePermissions(foreign,PosixFilePermissions.fromString("rw-------"));Files.createSymbolicLink(path,foreign);try{assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));}finally{Files.delete(path);}Files.createLink(path,foreign);try{assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));}finally{Files.delete(path);}assertArrayEquals(DATA,Files.readAllBytes(foreign));}
    }
    @Test void closedContentAndInvalidMetadataCannotBecomePathsOrAuthority(){
        var content=new PrivateEvidenceBlobs.Content(DATA);content.close();assertThrows(IllegalStateException.class,content::bytes);
        assertThrows(IllegalArgumentException.class,()->new PrivateEvidenceBlobs.Blob(null,1,"0".repeat(64)));assertThrows(IllegalArgumentException.class,()->new PrivateEvidenceBlobs.Blob(UUID.randomUUID(),0,"0".repeat(64)));assertThrows(IllegalArgumentException.class,()->new PrivateEvidenceBlobs.Blob(UUID.randomUUID(),1,"../private-path"));assertThrows(PrivateEvidenceBlobs.Rejected.class,()->store.put(null));assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(null));assertEquals("PrivateEvidenceBlob[redacted]",put().toString());
    }
    @Test void privateRootIdentitySurvivesRestartButTamperingFailsClosed()throws Exception{
        var blob=put();var restarted=new FileSystemPrivateEvidenceBlobs(root,List.of(temporary.resolve("public")));try(var content=restarted.read(blob)){assertArrayEquals(DATA,content.bytes());}
        Path marker=root.resolve(".private-evidence-identity");byte[] original=Files.readAllBytes(marker);byte[] changed=original.clone();changed[0]^=1;
        try{Files.write(marker,changed,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);assertThrows(PrivateEvidenceBlobs.Unavailable.class,()->store.read(blob));assertThrows(PrivateEvidenceBlobs.Unavailable.class,this::put);}finally{Files.write(marker,original,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);Arrays.fill(original,(byte)0);Arrays.fill(changed,(byte)0);}
        try(var content=store.read(blob)){assertArrayEquals(DATA,content.bytes());}
    }
}
