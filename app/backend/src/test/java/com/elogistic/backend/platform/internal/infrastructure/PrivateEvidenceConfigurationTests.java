package com.elogistic.backend.platform.internal.infrastructure;

import com.elogistic.backend.platform.api.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class PrivateEvidenceConfigurationTests {
    @Test void disabledConfigurationRequiresNoCredentialProviderOrStoragePath(){
        try(var context=new AnnotationConfigApplicationContext()){context.getEnvironment().setActiveProfiles("customer");context.register(PrivateEvidenceConfiguration.class);context.refresh();assertThrows(PrivateEvidenceStore.Unavailable.class,()->context.getBean(PrivateEvidenceStore.class).put(java.io.InputStream.nullInputStream()));assertThrows(PrivateEvidenceScreening.Unavailable.class,()->context.getBean(PrivateEvidenceScreening.class).inspect(new byte[0],"image/png"));}
    }
    @Test void explicitEnablementRejectsProductionAndMissingIndependentGates(){
        for(String[] profiles:List.of(new String[]{"customer"},new String[]{"customer","identity","postgres","production"},new String[]{"customer","identity","postgres","Production"}))try(var context=new AnnotationConfigApplicationContext()){
            context.getEnvironment().setActiveProfiles(profiles);context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("synthetic-flags",Map.of("support.claim-evidence.enabled","true","support.claim-intakes.enabled","true","identity.staff.enabled","true","identity.staff.environment","TEST","customer.rollout","SANDBOX")));context.register(PrivateEvidenceConfiguration.class);assertThrows(org.springframework.beans.BeansException.class,context::refresh);
        }
    }
}
