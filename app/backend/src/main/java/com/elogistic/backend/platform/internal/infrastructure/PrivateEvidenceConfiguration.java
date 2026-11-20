package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** No credentials, bucket policy change or network request is created by default. */
@Configuration @Profile("customer")
public class PrivateEvidenceConfiguration {
    private static boolean enabled(Environment env){
        if(!env.getProperty("support.claim-evidence.enabled",Boolean.class,false))return false;
        var profiles=Set.of(env.getActiveProfiles());
        if(!env.getProperty("support.claim-intakes.enabled",Boolean.class,false)||!env.getProperty("identity.staff.enabled",Boolean.class,false)
            ||!profiles.containsAll(Set.of("customer","identity","postgres"))||profiles.stream().anyMatch(p->Set.of("production","prod","demo","migration").contains(p.toLowerCase(java.util.Locale.ROOT)))
            ||!Set.of("LOCAL","TEST").contains(env.getProperty("identity.staff.environment","DISABLED"))||!"SANDBOX".equals(env.getProperty("customer.rollout")))throw new IllegalStateException("PRIVATE_EVIDENCE_LOCAL_TEST_REQUIRED");return true;
    }
    @Bean(name="claimPrivateS3Client",destroyMethod="close") S3Client privateS3Client(Environment env){
        if(!enabled(env)||!"s3".equals(env.getProperty("support.claim-evidence.mode","DISABLED")))return null;
        String region=env.getRequiredProperty("public.media.s3-region"),bucket=env.getRequiredProperty("public.media.s3-bucket");
        if(!"ap-southeast-2".equals(region)||!"elogistics-dev-media-160905".equals(bucket)
            ||java.util.stream.Stream.of("AWS_ENDPOINT_URL","AWS_ENDPOINT_URL_S3","aws.endpointUrl","aws.endpointUrl.s3").anyMatch(k->env.containsProperty(k)))throw new IllegalStateException("PRIVATE_EVIDENCE_APPROVED_DEV_BUCKET_REQUIRED");
        return S3Client.builder().region(Region.of(region)).overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(20)).apiCallAttemptTimeout(Duration.ofSeconds(10))).build();
    }
    @Bean("claimPrivateEvidenceStore") PrivateEvidenceStore store(Environment env,@org.springframework.beans.factory.annotation.Qualifier("claimPrivateS3Client") org.springframework.beans.factory.ObjectProvider<S3Client> client){
        if(!enabled(env))return new PrivateEvidenceStore(){public Blob put(java.io.InputStream in){throw new Unavailable();}public Content read(Blob expected){throw new Unavailable();}};
        return switch(env.getProperty("support.claim-evidence.mode","DISABLED")){
            case "s3"->new S3PrivateEvidenceStore(client.getObject(),env.getRequiredProperty("public.media.s3-bucket"));
            case "filesystem"->{
                // Deployer must enumerate every effective static/dev-server root, not only public/media.
                var roots=new java.util.ArrayList<Path>();roots.add(Path.of(env.getRequiredProperty("public.media.local-root")));
                for(String item:env.getRequiredProperty("support.claim-evidence.public-roots").split(";",-1)){if(item.isBlank())throw new IllegalStateException("PRIVATE_EVIDENCE_PUBLIC_ROOTS_REQUIRED");roots.add(Path.of(item));}
                yield new FileSystemPrivateEvidenceStore(Path.of(env.getRequiredProperty("support.claim-evidence.private-root")),roots);
            }
            default->throw new IllegalStateException("PRIVATE_EVIDENCE_CONFIGURATION_REQUIRED");
        };
    }
    @Bean("claimEvidenceScreening") PrivateEvidenceScreening scanner(Environment env){
        if(!enabled(env))return (bytes,mime)->{throw new PrivateEvidenceScreening.Unavailable();};
        return new ClamAvEvidenceScreening(env.getProperty("support.claim-evidence.clamav-host","127.0.0.1"),env.getProperty("support.claim-evidence.clamav-port",Integer.class,3310),env.getProperty("support.claim-evidence.scan-timeout-ms",Integer.class,5000));
    }
}
