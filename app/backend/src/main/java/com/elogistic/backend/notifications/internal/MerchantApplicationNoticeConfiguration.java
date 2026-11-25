package com.elogistic.backend.notifications.internal;

import com.elogistic.backend.identity.api.admin.AdminCommandAdmission;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.notifications.internal.infrastructure.*;
import com.elogistic.backend.platform.api.MerchantNoticeMail;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import tools.jackson.databind.json.JsonMapper;

/** Uses existing opt-in workflow gates; no secret, provider or SMTP configuration of its own. */
@Configuration @Profile("customer")
@ConditionalOnProperty(name={"merchant.application-workflow.enabled","merchant.admin-governance.enabled"},havingValue="true")
public class MerchantApplicationNoticeConfiguration {
    public MerchantApplicationNoticeConfiguration(Environment environment){
        Set<String> profiles=new HashSet<>();for(String profile:environment.getActiveProfiles())profiles.add(profile.toLowerCase(Locale.ROOT));
        if(!profiles.containsAll(Set.of("identity","customer","postgres"))||profiles.stream().anyMatch(Set.of("prod","production","demo","migration")::contains)
            ||!"SANDBOX".equals(environment.getProperty("customer.rollout"))||!Set.of("LOCAL","TEST").contains(environment.getProperty("identity.admin-dashboard.environment","DISABLED"))
            ||!environment.getProperty("identity.business-contact.enabled",Boolean.class,false)||!environment.getProperty("identity.admin-governance-step-up.enabled",Boolean.class,false))throw new IllegalStateException("MERCHANT_NOTICE_LOCAL_TEST_REQUIRED");
    }
    @Bean @DependsOnDatabaseInitialization
    JdbcMerchantApplicationNotices merchantApplicationNotices(DataSource source,CustomerAdmission customers,AdminCommandAdmission admins,MerchantNoticeMail transport,Environment environment){
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(source);jdbc.setQueryTimeout(3);
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('notification.merchant_application_notice') IS NOT NULL AND to_regprocedure('merchant.application_notice_bound(uuid,uuid,uuid,text)') IS NOT NULL",Boolean.class)))throw new IllegalStateException("MERCHANT_NOTICE_V43_REQUIRED");
        var keys=new HashMap<String,byte[]>();
        try{
            var root=JsonMapper.builder().enable(tools.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build().readTree(environment.getRequiredProperty("customer.security.delivery-encryption-keys"));
            if(root==null||!root.isObject()||root.isEmpty()||root.size()>16)throw new IllegalArgumentException();
            for(var field:root.properties()){if(!field.getValue().isString())throw new IllegalArgumentException();String encoded=field.getValue().asString();byte[] key=Base64.getDecoder().decode(encoded);if(!Base64.getEncoder().encodeToString(key).equals(encoded)){Arrays.fill(key,(byte)0);throw new IllegalArgumentException();}keys.put(field.getKey(),key);}
            return new JdbcMerchantApplicationNotices(source,customers,admins,new ProtectedMerchantNoticeRecipient(keys,environment.getRequiredProperty("customer.security.current-encryption-key")),transport,Clock.systemUTC());
        }finally{keys.values().forEach(key->Arrays.fill(key,(byte)0));}
    }
    @Bean NoticeSchedule merchantNoticeSchedule(JdbcMerchantApplicationNotices notices){return new NoticeSchedule(notices);}
    static final class NoticeSchedule {
        private final JdbcMerchantApplicationNotices notices;NoticeSchedule(JdbcMerchantApplicationNotices notices){this.notices=notices;}
        @Scheduled(fixedDelay=1000) public void dispatch(){try{for(int n=0;n<10&&notices.runOne();n++){/* Bounded claims; transport outside transaction. */}}catch(RuntimeException unavailable){org.slf4j.LoggerFactory.getLogger(NoticeSchedule.class).warn("MERCHANT_NOTICE_WORKER_UNAVAILABLE");}}
    }
}
