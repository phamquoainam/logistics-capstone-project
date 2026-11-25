package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.PrivateEvidenceStore;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.services.s3.S3Client;
import static org.junit.jupiter.api.Assertions.*;

class MerchantPrivateEvidenceConfigurationTests {
    @TempDir Path root;
    private MockEnvironment environment(){var e=new MockEnvironment();e.setActiveProfiles("customer","identity","postgres");return e.withProperty("customer.rollout","SANDBOX").withProperty("identity.admin-dashboard.environment","TEST").withProperty("merchant.application-workflow.enabled","true").withProperty("support.claim-evidence.mode","filesystem").withProperty("support.claim-evidence.private-root",root.resolve("private").toString()).withProperty("public.media.local-root",root.resolve("public").toString()).withProperty("support.claim-evidence.public-roots",root.resolve("static").toString());}
    @Test void merchantUsesCanonicalPrivateStoreWithoutEnablingClaimOrStaffAuthority(){
        var e=environment();var provider=new DefaultListableBeanFactory().getBeanProvider(S3Client.class);var store=new PrivateEvidenceConfiguration().store(e,provider);
        byte[] input="Synthetic isolated private bytes".getBytes(java.nio.charset.StandardCharsets.US_ASCII);var blob=store.put(new ByteArrayInputStream(input));try(var read=store.read(blob)){assertArrayEquals(input,read.bytes());}
        assertFalse(e.getProperty("support.claim-evidence.enabled",Boolean.class,false));assertFalse(e.getProperty("support.claim-intakes.enabled",Boolean.class,false));assertFalse(e.getProperty("identity.staff.enabled",Boolean.class,false));
    }
    @Test void claimSecurityGatesAndProductionDenialRemainIndependent(){
        var factory=new PrivateEvidenceConfiguration();var provider=new DefaultListableBeanFactory().getBeanProvider(S3Client.class);
        assertThrows(IllegalStateException.class,()->factory.store(environment().withProperty("support.claim-evidence.enabled","true"),provider));
        var production=environment();production.addActiveProfile("production");assertThrows(IllegalStateException.class,()->factory.store(production,provider));
        var disabled=factory.store(environment().withProperty("merchant.application-workflow.enabled","false"),provider);assertThrows(PrivateEvidenceStore.Unavailable.class,()->disabled.put(new ByteArrayInputStream(new byte[]{1})));
    }
}
