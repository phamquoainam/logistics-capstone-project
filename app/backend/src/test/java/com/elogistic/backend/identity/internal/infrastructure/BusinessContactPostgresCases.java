package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.*;
import com.elogistic.backend.identity.internal.application.port.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Isolated PG18 core/protected SMTP tests. Trusted test request ports are not HTTP acceptance. */
public final class BusinessContactPostgresCases {
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;private final UUID account;
    private final CustomerAdmission admission;private final CustomerProofProtection protection;
    private final JdbcCustomerBusinessContacts contacts;private final IdentitySessionCommands.AccessGrant access;
    private com.elogistic.backend.merchant.api.customer.MerchantApplications workflow;
    private com.elogistic.backend.platform.api.PrivateEvidenceStore evidenceStore;
    public com.elogistic.backend.platform.api.PrivateEvidenceStore evidenceStore(){return Objects.requireNonNull(evidenceStore);}
    private static final String CONTACT="business-contact@example.invalid";
    private static byte[] key(int n){byte[] key=new byte[32];Arrays.fill(key,(byte)n);return key;}
    public BusinessContactPostgresCases(DataSource source,UUID account,String password,Clock clock){
        this.source=source;this.jdbc=new JdbcTemplate(source);this.clock=clock;this.account=account;
        var store=new JdbcIdentitySessionStore(source,IdentitySessionStorage.CUSTOMER);
        String email=jdbc.queryForObject("SELECT email FROM identity.user_account WHERE id=?",String.class,account);
        try(var logged=IdentitySessions.customer(store,new ProtectedSessionCredentials(),(o,r,s,p)->{},clock)
            .loginIdentity(new IdentitySessionCommands.Login(email,password,UUID.randomUUID()))){access=logged.access();}
        admission=new CustomerAdmissions(store,encoded->{
            if(!"trusted-synthetic-customer-claim".equals(encoded))throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);
            return new CustomerAccessVerifier.Claim(access.sessionId(),account,access.authVersion(),access.issuedAt(),access.expiresAt());
        },(request,session)->assertEquals(access.sessionId(),session),clock);
        protection=new ProtectedCustomerProofs(Map.of("v1",key(71)),"v1",Map.of("e1",key(72)),"e1");
        contacts=recompose(true,"TEST");
    }
    public boolean businessWorkspaceVisible(UUID organization){
        var workspaces=new com.elogistic.backend.merchant.internal.infrastructure.JdbcBusinessWorkspaces(source,true,"TEST");
        return run(c->{var access=new com.elogistic.backend.merchant.api.customer.BusinessWorkspaces.Access(){public UUID accountId(){admission.requireCurrent(c);return c.actor().accountId();}public void requireCurrent(){admission.requireCurrent(c);}public boolean eligibleAccount(UUID id){admission.requireCurrent(c);return c.actor().accountId().equals(id)&&c.actor().eligible();}};return workspaces.list(access).items().stream().anyMatch(value->value.organizationId().equals(organization));});
    }
    public void noticeWithdrawalAndReaderDenials(com.elogistic.backend.merchant.api.MerchantApplicationNoticeEvents events,com.elogistic.backend.workflow.internal.application.MerchantNoticeComposition notices){
        var input=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Input(null,null,"Synthetic withdrawal notice",null,null,null,null,null,null,null,null,null);
        var draft=run(c->workflow.create(c,input,UUID.randomUUID()));UUID key=UUID.randomUUID();var reason=new com.elogistic.backend.merchant.api.customer.MerchantApplications.ReasonInput("Synthetic notice withdrawal");
        var escaped=new java.util.concurrent.atomic.AtomicReference<CustomerAdmission.Context>();
        var withdrawn=run(c->{escaped.set(c);var result=workflow.withdraw(c,draft.id(),reason,1,key);
            assertEquals("WITHDRAWN",events.commandEvent(c,draft.id(),"WITHDRAW",key).orElseThrow().template());
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->events.commandEvent(c,draft.id(),"APPROVE",key));
            var constructed=new CustomerAdmission.Context(c.requestId(),c.sessionId(),c.actor(),c.subjects());
            assertThrows(IdentityCommandFailure.class,()->events.commandEvent(constructed,draft.id(),"WITHDRAW",key));
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->events.commandEvent(c,UUID.randomUUID(),"WITHDRAW",key));
            notices.after(c,draft.id(),"WITHDRAW",key);notices.after(c,draft.id(),"WITHDRAW",key);return result;
        });
        assertEquals("WITHDRAWN",withdrawn.state());
        run(c->{assertEquals(withdrawn,workflow.withdraw(c,draft.id(),reason,1,key));assertTrue(events.commandEvent(c,draft.id(),"WITHDRAW",key).isEmpty());notices.after(c,draft.id(),"WITHDRAW",key);return null;});
        assertThrows(IdentityCommandFailure.class,()->events.commandEvent(escaped.get(),draft.id(),"WITHDRAW",key));
    }
    public CustomerBusinessContacts contactFacts(){return contacts;}
    public CustomerAdmission customerAdmission(){return admission;}
    private JdbcCustomerBusinessContacts recompose(boolean enabled,String env){return new JdbcCustomerBusinessContacts(source,admission,protection,new JdbcIdentityRateLimiter(source,key(73)),()->"127.0.0.1",clock,enabled,env);}
    private <T>T run(Function<CustomerAdmission.Context,T> work){return admission.execute("trusted-synthetic-customer-claim",UUID.randomUUID(),Set.of(),work);}
    public void workflowDraftCompatibilityAndDenial(){
        var store=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationStore(source);
        var service=new com.elogistic.backend.merchant.internal.application.MerchantApplicationService(admission,store,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key(91)),"v1"),clock);
        var input=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Input(null,null,"Synthetic draft",null,null,null,null,null,null,null,null,null);
        UUID request=UUID.randomUUID();var created=run(c->service.create(c,input,request));assertEquals("DRAFT",created.state());
        var next=run(c->service.update(c,created.id(),input,1,UUID.randomUUID()));assertEquals(2,next.version());
        assertEquals(created,run(c->service.create(c,input,request)));assertEquals(next,run(c->service.get(c,created.id())));
        assertThrows(org.springframework.dao.DataAccessException.class,()->run(c->{jdbc.update("UPDATE merchant.application SET state='SUBMITTED',version=version+1 WHERE id=?",created.id());return null;}));
        assertThrows(org.springframework.dao.DataAccessException.class,()->run(c->{jdbc.update("UPDATE merchant.application SET version=version+1 WHERE id=?",created.id());jdbc.update("INSERT INTO merchant.application_draft_version SELECT id,version,applicant_account_id,draft,created_at,updated_at FROM merchant.application WHERE id=?",created.id());return null;}));
        assertEquals(next,run(c->service.get(c,created.id())));assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM merchant.application_draft_version WHERE application_id=?",Integer.class,created.id()));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO merchant.application(id,applicant_account_id,state,version,draft,created_at,updated_at) VALUES (?,?,'SUBMITTED',1,'{}',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",UUID.randomUUID(),account));
        assertFalse(jdbc.queryForObject("SELECT has_function_privilege(current_user,'merchant.guard_governance_system_eligibility()','EXECUTE')",Boolean.class));
    }
    public UUID screenedEvidenceAndImmutableSubmission()throws Exception{
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.membership",Integer.class));
        return screenedEvidenceAndImmutableSubmissionPreservingOrganizations();
    }
    public UUID screenedEvidenceAndImmutableSubmissionPreservingOrganizations()throws Exception{return screenedEvidenceAndImmutableSubmissionPreservingOrganizations("SYNTHETIC-REG-1");}
    public UUID screenedEvidenceAndImmutableSubmissionPreservingOrganizations(String registration)throws Exception{
        var originalOrganizations=jdbc.queryForList("SELECT id FROM merchant.organization ORDER BY id",UUID.class);
        var originalMemberships=jdbc.queryForList("SELECT id FROM merchant.membership ORDER BY id",UUID.class);
        var privateParent=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of(System.getenv("TASK_C_PRIVATE_DIRECTORY")),"merchant-private-");
        var disk=new com.elogistic.backend.platform.internal.infrastructure.FileSystemPrivateEvidenceStore(privateParent.resolve("evidence"),List.of(privateParent.resolve("public")));
        evidenceStore=disk;
        var fingerprints=new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key(91)),"v1");
        try(var daemon=new com.elogistic.backend.testsupport.ClamAvTestDaemon()){
            var scanning=new com.elogistic.backend.platform.internal.infrastructure.ClamAvEvidenceScreening("127.0.0.1",daemon.port(),2000);
            var service=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationWorkflow(source,admission,contacts,fingerprints,disk,scanning,new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",key(72)),"e1"),clock,true,"TEST");workflow=service;
            var address=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Address("VN","Synthetic area","Synthetic locality","Synthetic private address");
            var declaration=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Declaration(true,true,"SYNTHETIC-CONSENT-1");
            var initial=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Input("COMPANY","SYNTHETIC-J1","Synthetic legal name "+"N".repeat(200),registration,address,address,"Synthetic Representative",CONTACT,"Authorized representative",null,List.of(),declaration);
            UUID createKey=UUID.randomUUID();var created=run(c->service.create(c,initial,createKey));UUID app=created.id();
            var requirements=run(c->service.requirements(c,app));assertEquals(app,requirements.applicationId());assertEquals("1",requirements.policyVersion());assertEquals("SYNTHETIC-CONSENT-1",requirements.consentVersion());assertEquals("Synthetic accuracy statement",requirements.consentStatements().accuracy());assertEquals("Synthetic representation statement",requirements.consentStatements().representation());
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->run(c->service.requirements(c,UUID.randomUUID())));
            var accepted=run(c->contacts.request(c,app,CONTACT,UUID.randomUUID()));byte[] token;
            try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
                var delivery=new JdbcCustomerDeliveryStore(source,protection,()->(x,p,o,a,n,e)->false,true);
                var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"delivery@example.invalid",Set.of("example.invalid"),"https://customer.example.invalid",true),clock);
                assertTrue(new CustomerDeliveryWorker(delivery,protection,smtp,clock).runOne());assertEquals(1,inbox.messages().size());token=mailToken(inbox.messages().getFirst(),app,accepted.requestId());
            }
            com.elogistic.backend.identity.api.customer.CustomerBusinessContacts.BusinessContactAttestation fact;
            try{fact=run(c->contacts.confirm(c,app,accepted.requestId(),token,UUID.randomUUID()));}finally{Arrays.fill(token,(byte)0);}
            var image=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",image);byte[] bytes=image.toByteArray();
            UUID firstKey=UUID.randomUUID();var first=service.uploadEvidence("trusted-synthetic-customer-claim",UUID.randomUUID(),app,"REGISTRATION","image/png",bytes,firstKey);
            var repeat=service.uploadEvidence("trusted-synthetic-customer-claim",UUID.randomUUID(),app,"REGISTRATION","image/png",bytes,firstKey);assertEquals(first,repeat);assertEquals(1,daemon.requests.get());
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->service.uploadEvidence("trusted-synthetic-customer-claim",UUID.randomUUID(),app,"REPRESENTATIVE_AUTHORIZATION","image/png",bytes,firstKey));
            var second=service.uploadEvidence("trusted-synthetic-customer-claim",UUID.randomUUID(),app,"REPRESENTATIVE_AUTHORIZATION","image/png",bytes,UUID.randomUUID());assertEquals(2,daemon.requests.get());
            assertEquals("CLEAN",first.state());assertEquals("CLEAN",second.state());assertNotEquals(first.id(),second.id());
            var full=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Input(initial.entityType(),initial.jurisdiction(),initial.registeredName(),initial.legalIdentifier(),address,address,initial.representativeName(),CONTACT,initial.representativeRelationship(),fact.id(),List.of(first.id(),second.id()),declaration);
            var updated=run(c->service.update(c,app,full,1,UUID.randomUUID()));assertEquals(2,updated.version());
            var input=new com.elogistic.backend.merchant.api.customer.MerchantApplications.SubmissionInput("Synthetic shorter display name");UUID submitKey=UUID.randomUUID();
            var submitted=run(c->service.submit(c,app,input,2,submitKey));assertEquals("SUBMITTED",submitted.state());assertEquals(3,submitted.version());assertEquals(1,submitted.revision());
            assertEquals(submitted,run(c->service.get(c,app)));assertEquals(submitted,run(c->service.submit(c,app,input,2,submitKey)));assertEquals(created,run(c->service.create(c,initial,createKey)));
            assertThrows(com.elogistic.backend.merchant.api.GovernanceFailure.class,()->run(c->service.submit(c,app,new com.elogistic.backend.merchant.api.customer.MerchantApplications.SubmissionInput("Changed display"),2,submitKey)));
            assertThrows(com.elogistic.backend.merchant.api.OnboardingFailure.class,()->run(c->service.update(c,app,full,3,UUID.randomUUID())));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_revision WHERE application_id=?",Integer.class,app));
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM merchant.application_revision_evidence WHERE application_id=?",Integer.class,app));
            assertEquals(full.registeredName(),jdbc.queryForObject("SELECT submitted_input->>'registeredName' FROM merchant.application_revision WHERE application_id=?",String.class,app));
            assertEquals(input.organizationDisplayName(),jdbc.queryForObject("SELECT activation_display_name FROM merchant.application_revision WHERE application_id=?",String.class,app));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.business_contact_attestation WHERE id=? AND used_revision=1 AND used_at IS NOT NULL",Integer.class,fact.id()));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.governance_receipt WHERE target_id=? AND operation='SUBMIT' AND input_key_id='v1'",Integer.class,app));
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM merchant.application_draft_version WHERE application_id=?",Integer.class,app));
            assertEquals(originalOrganizations,jdbc.queryForList("SELECT id FROM merchant.organization ORDER BY id",UUID.class));assertEquals(originalMemberships,jdbc.queryForList("SELECT id FROM merchant.membership ORDER BY id",UUID.class));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application_revision SET activation_display_name='tampered' WHERE application_id=?",app));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application_evidence SET state='ARCHIVED',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",first.id()));
            assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE merchant.application SET state='APPROVED',version=version+1,terminal_at=CURRENT_TIMESTAMP WHERE id=?",app));
            return app;
        }
    }
    public void resubmitWithFreshContactFact(UUID app)throws Exception{
        var originalDigest=jdbc.queryForObject("SELECT content_digest FROM merchant.application_revision WHERE application_id=? AND revision=1",byte[].class,app);
        var before=run(c->workflow.get(c,app));assertEquals("NEEDS_INFORMATION",before.state());
        try(var p=pending(app)){
            var fact=run(c->contacts.confirm(c,app,p.proof(),p.token(),UUID.randomUUID()));var d=before.draft();
            var changed=new com.elogistic.backend.merchant.api.customer.MerchantApplications.Input(d.entityType(),d.jurisdiction(),d.registeredName(),d.legalIdentifier(),d.registeredAddress(),d.operatingAddress(),d.representativeName(),d.representativeEmail(),d.representativeRelationship(),fact.id(),d.evidenceIds(),d.declarations());
            var updated=run(c->workflow.update(c,app,changed,before.version(),UUID.randomUUID()));UUID key=UUID.randomUUID();
            var input=new com.elogistic.backend.merchant.api.customer.MerchantApplications.SubmissionInput("Synthetic corrected display");
            var result=run(c->workflow.resubmit(c,app,input,updated.version(),key));assertEquals(2,result.revision());assertEquals("SUBMITTED",result.state());assertEquals(before.version()+2,result.version());
            assertEquals(result,run(c->workflow.resubmit(c,app,input,updated.version(),key)));
            assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM merchant.application_revision WHERE application_id=?",Integer.class,app));
            assertArrayEquals(originalDigest,jdbc.queryForObject("SELECT content_digest FROM merchant.application_revision WHERE application_id=? AND revision=1",byte[].class,app));
            var event=jdbc.queryForMap("SELECT id,protected_payload FROM merchant.governance_event WHERE application_id=? AND operation='RESUBMIT'",app);
            var payloads=new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",key(72)),"e1");
            var value=new tools.jackson.databind.ObjectMapper().readTree(payloads.open((UUID)event.get("id"),app,2,"RESUBMIT",(byte[])event.get("protected_payload")));
            assertEquals(fact.verifiedAt().toString(),value.get("contactFact").get("verifiedAt").asString());assertEquals(fact.id().toString(),value.get("contactFact").get("id").asString());
        }
    }
    private int count(String table){return jdbc.queryForObject("SELECT count(*) FROM identity."+table,Integer.class);}
    private byte[] mailToken(byte[] wire,UUID app,UUID proof)throws Exception{
        var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),new ByteArrayInputStream(wire));
        String text=CustomerSmtpTestInbox.plainText(message);
        assertTrue(text.contains("ArgoPost"));assertFalse(text.contains("movapost"));
        assertTrue(text.contains("#applicationId="+app+"&proofId="+proof+"&proof="),"LINK_MUST_BIND_REAL_APPLICATION_AND_PROOF");
        var matcher=java.util.regex.Pattern.compile("&proof=([A-Za-z0-9_-]{43})(?:\\s|$)").matcher(text);
        assertTrue(matcher.find(),"SYNTHETIC_CONTACT_PROOF_MISSING");return matcher.group(1).getBytes(StandardCharsets.US_ASCII);
    }
    public void smtpClearingConfirmationAndAtomicConsume()throws Exception{
        UUID app=UUID.randomUUID(),request=UUID.randomUUID(),confirm=UUID.randomUUID();
        var accepted=run(c->contacts.request(c,app,CONTACT,request));
        assertTrue(accepted.accepted());assertEquals(0,count("business_contact_attestation"));
        assertEquals(accepted,run(c->recompose(true,"TEST").request(c,app,CONTACT,request)));
        assertEquals(1,count("customer_proof"));assertEquals(1,count("customer_delivery"));assertEquals(1,count("governance_proof_receipt"));
        var store=new JdbcCustomerDeliveryStore(source,protection,()->(i,p,o,a,n,e)->false,true);
        byte[] token;
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
            var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"delivery@example.invalid",Set.of("example.invalid"),"https://customer.example.invalid",true),clock);
            assertTrue(new CustomerDeliveryWorker(store,protection,smtp,clock).runOne());
            assertEquals("ACCEPTED_BY_PROVIDER",jdbc.queryForObject("SELECT status FROM identity.customer_delivery WHERE proof_id=?",String.class,accepted.requestId()));
            assertTrue(jdbc.queryForObject("SELECT nonce IS NULL AND ciphertext IS NULL AND recipient_nonce IS NULL AND recipient_ciphertext IS NULL FROM identity.customer_delivery WHERE proof_id=?",Boolean.class,accepted.requestId()));
            var messages=inbox.messages();assertEquals(1,messages.size());token=mailToken(messages.getFirst(),app,accepted.requestId());
        }
        try{
            var fact=run(c->recompose(true,"TEST").confirm(c,app,accepted.requestId(),token,confirm));
            assertEquals(fact,run(c->contacts.confirm(c,app,accepted.requestId(),token,confirm)));
            assertEquals(app,fact.applicationId());assertEquals(1,count("business_contact_attestation"));assertEquals(2,count("governance_proof_receipt"));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.audit_entry WHERE event_type='BUSINESS_CONTACT_VERIFIED'",Integer.class));
            assertThrows(IdentityCommandFailure.class,()->run(c->{contacts.consumeAttestation(c,fact.id(),app,1,"other@example.invalid");return null;}));
            assertThrows(IllegalStateException.class,()->run(c->{contacts.consumeAttestation(c,fact.id(),app,1,CONTACT);throw new IllegalStateException("SYNTHETIC_SUBMISSION_ROLLBACK");}));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.business_contact_attestation WHERE used_at IS NOT NULL",Integer.class));
            run(c->{contacts.consumeAttestation(c,fact.id(),app,1,CONTACT);return null;});
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.business_contact_attestation WHERE used_revision=1 AND used_at IS NOT NULL",Integer.class));
            assertThrows(IdentityCommandFailure.class,()->run(c->{contacts.consumeAttestation(c,fact.id(),app,2,CONTACT);return null;}));
        }finally{Arrays.fill(token,(byte)0);}
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.customer_identifier WHERE account_id=?",Integer.class,account),"CONTACT_IS_NOT_A_NEW_IDENTITY_CREDENTIAL");
        assertFalse(new CustomerDeliveryWorker(store,protection,m->{fail("REPLAY_MUST_NOT_SEND_MAIL");return null;},clock).runOne());
    }
    public void submittedFactsPreserveHistoryAndRejectWrongBindings(com.elogistic.backend.identity.api.admin.AdminCommandAdmission commands)throws Exception{
        var refs=new com.elogistic.backend.identity.api.admin.AdminCommandAdmission.ReferencedSubjects(Set.of(account),Set.of());
        try(var p=pending()){
            var fact=run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID()));
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.submittedAttestation(c,fact.id(),p.application(),1,CONTACT)));
            assertThrows(IdentityCommandFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)));
            int audits=count("audit_entry"),receipts=count("governance_proof_receipt"),deliveries=count("customer_delivery");
            assertThrows(IllegalStateException.class,()->run(c->{contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);assertEquals(fact,contacts.submittedAttestation(c,fact.id(),p.application(),1,CONTACT));throw new IllegalStateException("SYNTHETIC_ROLLBACK");}));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.business_contact_attestation WHERE used_at IS NOT NULL",Integer.class));
            assertEquals(fact,run(c->{contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);return contacts.submittedAttestation(c,fact.id(),p.application(),1,CONTACT);}));
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.submittedAttestation(c,fact.id(),p.application(),2,CONTACT)));
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.submittedAttestation(c,fact.id(),UUID.randomUUID(),1,CONTACT)));
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.submittedAttestation(c,fact.id(),p.application(),1,"wrong@example.invalid")));
            var escaped=new java.util.concurrent.atomic.AtomicReference<CustomerAdmission.Context>();run(c->{escaped.set(c);return null;});assertThrows(IdentityCommandFailure.class,()->contacts.submittedAttestation(escaped.get(),fact.id(),p.application(),1,CONTACT));
            var nested=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source));nested.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            run(c->{assertThrows(IdentityCommandFailure.class,()->nested.execute(status->contacts.submittedAttestation(c,fact.id(),p.application(),1,CONTACT)));return null;});
            commands.execute(UUID.randomUUID(),refs,c->{assertEquals(fact,contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT));
                var forwarded=(com.elogistic.backend.identity.api.admin.AdminCommandContext)java.lang.reflect.Proxy.newProxyInstance(com.elogistic.backend.identity.api.admin.AdminCommandContext.class.getClassLoader(),new Class<?>[]{com.elogistic.backend.identity.api.admin.AdminCommandContext.class},(proxy,m,a)->m.invoke(c,a));
                assertThrows(com.elogistic.backend.identity.api.admin.AdminFailure.class,()->contacts.submittedAttestation(forwarded,fact.id(),p.application(),account,1,CONTACT));
                assertThrows(com.elogistic.backend.identity.api.admin.AdminFailure.class,()->nested.execute(status->contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)));return null;});
            assertThrows(com.elogistic.backend.identity.api.admin.AdminFailure.class,()->commands.execute(UUID.randomUUID(),new com.elogistic.backend.identity.api.admin.AdminCommandAdmission.ReferencedSubjects(Set.of(),Set.of()),c->contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)));
            var historical=recomposeAt(Clock.fixed(fact.expiresAt().plusSeconds(3600),ZoneOffset.UTC));
            jdbc.update("UPDATE identity.customer_auth_session SET revoked_at=clock_timestamp() WHERE id=?",access.sessionId());
            assertEquals(fact,commands.execute(UUID.randomUUID(),refs,c->historical.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)),"ELAPSED_PROOF_TTL_AND_ORIGINAL_SESSION_LOGOUT_DO_NOT_ERASE_USED_FACT");
            assertEquals(audits,count("audit_entry"));assertEquals(receipts,count("governance_proof_receipt"));assertEquals(deliveries,count("customer_delivery"));
            jdbc.update("UPDATE identity.business_contact_attestation SET revoked_at=clock_timestamp() WHERE id=?",fact.id());
            assertThrows(IdentityCommandFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)));
        }
    }
    public void submittedFactSecurityVersionChangeDenies(com.elogistic.backend.identity.api.admin.AdminCommandAdmission commands)throws Exception{
        try(var p=pending()){
            var fact=run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID()));run(c->{contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);return null;});
            jdbc.update("UPDATE identity.user_account SET auth_version=auth_version+1 WHERE id=?",account);
            var refs=new com.elogistic.backend.identity.api.admin.AdminCommandAdmission.ReferencedSubjects(Set.of(account),Set.of());
            assertThrows(IdentityCommandFailure.class,()->commands.execute(UUID.randomUUID(),refs,c->contacts.submittedAttestation(c,fact.id(),p.application(),account,1,CONTACT)));
        }
    }
    private record Pending(UUID application,UUID proof,byte[] token) implements AutoCloseable {
        public void close(){Arrays.fill(token,(byte)0);}
        public String toString(){return "Synthetic contact proof[redacted]";}
    }
    private Pending pending(){
        return pending(UUID.randomUUID());
    }
    private Pending pending(UUID app){
        var result=run(c->contacts.request(c,app,CONTACT,UUID.randomUUID()));
        var captured=new java.util.concurrent.atomic.AtomicReference<byte[]>();
        var store=new JdbcCustomerDeliveryStore(source,protection,()->(i,p,o,a,n,e)->false,true);
        assertTrue(new CustomerDeliveryWorker(store,protection,m->{
            assertEquals(app,m.applicationBinding());captured.set(m.token());
            return new CustomerDeliveryTransport.Outcome(CustomerDeliveryTransport.Kind.ACCEPTED_BY_PROVIDER,"TEST:"+m.id(),true);
        },clock).runOne());
        assertNotNull(captured.get());return new Pending(app,result.requestId(),captured.get());
    }
    public void wrongBindingExpiryAndTamperDeny()throws Exception{
        try(var p=pending()){
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.confirm(c,UUID.randomUUID(),p.proof(),p.token(),UUID.randomUUID())));
            byte[] wrong=p.token().clone();wrong[0]=wrong[0]=='A'?(byte)'B':(byte)'A';
            try{assertThrows(IdentityCommandFailure.class,()->run(c->contacts.confirm(c,p.application(),p.proof(),wrong,UUID.randomUUID())));}finally{Arrays.fill(wrong,(byte)0);}
            assertEquals(0,count("business_contact_attestation"));
            var result=run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID()));
            assertThrows(IdentityCommandFailure.class,()->run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID())));
            assertThrows(IdentityCommandFailure.class,()->run(c->{contacts.consumeAttestation(c,result.id(),UUID.randomUUID(),1,CONTACT);return null;}));
            Instant expiry=result.expiresAt();var later=recomposeAt(Clock.fixed(expiry,ZoneOffset.UTC));
            assertThrows(IdentityCommandFailure.class,()->run(c->{later.consumeAttestation(c,result.id(),p.application(),1,CONTACT);return null;}));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM identity.business_contact_attestation WHERE used_at IS NOT NULL",Integer.class));
        }
        UUID id=UUID.randomUUID();Instant created=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);var proof=com.elogistic.backend.identity.internal.domain.CustomerProof.issue(id,account,com.elogistic.backend.identity.internal.domain.CustomerProof.Purpose.BUSINESS_CONTACT,created,access.sessionId(),access.authVersion(),null);
        try(var sealed=protection.issue(proof,CONTACT)){
            byte[] envelope=GovernanceRecipientEnvelope.seal(sealed);
            try{
                assertEquals(CONTACT,GovernanceRecipientEnvelope.open(protection,proof,sealed.verifierKey(),envelope,created));
                envelope[envelope.length-1]^=1;
                assertThrows(RuntimeException.class,()->GovernanceRecipientEnvelope.open(protection,proof,sealed.verifierKey(),envelope,created));
            }finally{Arrays.fill(envelope,(byte)0);}
        }
    }
    private JdbcCustomerBusinessContacts recomposeAt(Clock time){return new JdbcCustomerBusinessContacts(source,admission,protection,new JdbcIdentityRateLimiter(source,key(73)),()->"127.0.0.1",time,true,"TEST");}
    public void suspendedTransactionAndRevokedSessionDeny()throws Exception{
        UUID app=UUID.randomUUID(),key=UUID.randomUUID();run(c->contacts.request(c,app,CONTACT,key));
        var nested=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.support.JdbcTransactionManager(source));
        nested.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        run(c->{assertThrows(IdentityCommandFailure.class,()->nested.execute(status->contacts.request(c,app,CONTACT,key)));return null;});
        jdbc.update("UPDATE identity.customer_auth_session SET revoked_at=clock_timestamp() WHERE id=?",access.sessionId());
        assertThrows(IdentityCommandFailure.class,()->run(c->contacts.request(c,app,CONTACT,key)));
        assertEquals(1,count("governance_proof_receipt"));
    }
    public void wrongSubjectAndSessionDeny(UUID other,String password)throws Exception{
        try(var p=pending()){
            var otherSubject=new BusinessContactPostgresCases(source,other,password,clock);
            assertThrows(IdentityCommandFailure.class,()->otherSubject.run(c->otherSubject.contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID())));
            var anotherSession=new BusinessContactPostgresCases(source,account,password,clock);
            assertThrows(IdentityCommandFailure.class,()->anotherSession.run(c->anotherSession.contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID())));
            assertEquals(0,count("business_contact_attestation"));
            var fact=run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),UUID.randomUUID()));
            assertThrows(IdentityCommandFailure.class,()->anotherSession.run(c->{anotherSession.contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);return null;}));
            assertThrows(IdentityCommandFailure.class,()->otherSubject.run(c->{otherSubject.contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);return null;}));
            run(c->{contacts.consumeAttestation(c,fact.id(),p.application(),1,CONTACT);return null;});
        }
    }
    public void concurrentConfirmationAndReceiptBindingDmlDeny()throws Exception{
        try(var p=pending();var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            UUID key=UUID.randomUUID();var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<CustomerBusinessContacts.BusinessContactAttestation> task=()->{
                ready.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("CONTACT_RACE_NOT_STARTED");
                return run(c->contacts.confirm(c,p.application(),p.proof(),p.token(),key));
            };
            var first=pool.submit(task);var second=pool.submit(task);assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown();
            assertEquals(first.get(10,java.util.concurrent.TimeUnit.SECONDS),second.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,count("business_contact_attestation"));assertEquals(2,count("governance_proof_receipt"));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM identity.audit_entry WHERE event_type='BUSINESS_CONTACT_VERIFIED'",Integer.class));
            var failure=assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO identity.governance_proof_receipt(actor_id,operation,operation_key,customer_session_id,auth_version,target_id,input_key_id,input_digest,customer_proof_id,protected_address,created_at,expires_at) SELECT actor_id,operation,?,customer_session_id,auth_version,?,input_key_id,input_digest,customer_proof_id,protected_address,created_at,expires_at FROM identity.governance_proof_receipt WHERE operation='REQUEST_BUSINESS_CONTACT' AND customer_proof_id=?",UUID.randomUUID(),UUID.randomUUID(),p.proof()));
            Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();assertInstanceOf(java.sql.SQLException.class,cause);assertEquals("23505",((java.sql.SQLException)cause).getSQLState());
            assertEquals(2,count("governance_proof_receipt"));
        }
    }
    private static final String HTTP_ORIGIN="https://customer.example.invalid";
    private static final tools.jackson.databind.json.JsonMapper JSON=tools.jackson.databind.json.JsonMapper.builder().build();
    private record HttpBrowser(String access,jakarta.servlet.http.Cookie csrf,jakarta.servlet.http.Cookie refresh,String challenge){
        public String toString(){return "Synthetic HTTP Customer[redacted]";}
    }
    private jakarta.servlet.http.Cookie cookie(org.springframework.mock.web.MockHttpServletResponse r,String name){
        String header=r.getHeaders("Set-Cookie").stream().filter(h->h.startsWith(name+"=")).findFirst().orElseThrow();
        return new jakarta.servlet.http.Cookie(name,header.substring(name.length()+1,header.indexOf(';')));
    }
    private HttpBrowser login(org.springframework.test.web.servlet.MockMvc mvc,UUID subject,String password)throws Exception{
        var csrf=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer-auth/csrf").secure(true)).andReturn().getResponse();assertEquals(200,csrf.getStatus());
        String challenge=JSON.readTree(csrf.getContentAsString()).get("challenge").asString();
        var logged=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/customer-auth/login").secure(true).header("Origin",HTTP_ORIGIN).header("X-CSRF-Token",challenge).cookie(cookie(csrf,"__Host-elogistics.customer.csrf")).contentType("application/json").content(JSON.writeValueAsString(Map.of("email",jdbc.queryForObject("SELECT email FROM identity.user_account WHERE id=?",String.class,subject),"password",password)))).andReturn().getResponse();assertEquals(200,logged.getStatus());
        var refresh=cookie(logged,"__Host-elogistics.customer.refresh");var bound=cookie(logged,"__Host-elogistics.customer.csrf");
        var rebound=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer-auth/csrf").secure(true).cookie(bound,refresh)).andReturn().getResponse();assertEquals(200,rebound.getStatus());
        return new HttpBrowser(JSON.readTree(logged.getContentAsString()).get("accessToken").asString(),cookie(rebound,"__Host-elogistics.customer.csrf"),refresh,JSON.readTree(rebound.getContentAsString()).get("challenge").asString());
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(HttpBrowser browser,String path,String body,UUID key){
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.challenge()).header("Idempotency-Key",key.toString()).cookie(browser.csrf(),browser.refresh()).contentType("application/json").content(body);
    }
    private UUID draft(org.springframework.test.web.servlet.MockMvc http,HttpBrowser browser)throws Exception{
        var result=http.perform(post(browser,"/api/v1/merchant-applications","{}",UUID.randomUUID())).andReturn().getResponse();assertEquals(201,result.getStatus());return UUID.fromString(JSON.readTree(result.getContentAsString()).get("id").asString());
    }
    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(HttpBrowser browser,String path,byte[] bytes,UUID key){
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(path).file(new org.springframework.mock.web.MockMultipartFile("file","synthetic.png","image/png",bytes)).param("purpose","REGISTRATION").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+browser.access()).header("X-CSRF-Token",browser.challenge()).header("Idempotency-Key",key.toString()).cookie(browser.csrf(),browser.refresh());
    }
    public void httpEvidenceOwnershipCsrfAndStrictSubmission(UUID other,String password)throws Exception{
        var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(3072);var pair=generator.generateKeyPair();
        var tokens=new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://customer-identity.example.invalid","customer-browser","contact-test",(java.security.interfaces.RSAPrivateKey)pair.getPrivate(),Map.of("contact-test",(java.security.interfaces.RSAPublicKey)pair.getPublic()),clock);
        var privateParent=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of(System.getenv("TASK_C_PRIVATE_DIRECTORY")),"merchant-http-private-");
        var disk=new com.elogistic.backend.platform.internal.infrastructure.FileSystemPrivateEvidenceStore(privateParent.resolve("evidence"),List.of(privateParent.resolve("public")));
        try(var inbox=new CustomerSmtpTestInbox(0,false,false,250,null);var daemon=new com.elogistic.backend.testsupport.ClamAvTestDaemon()){
            var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"delivery@example.invalid",Set.of("example.invalid"),HTTP_ORIGIN,true),clock);
            var services=com.elogistic.backend.identity.internal.ProductionCustomerComposition.create(source,tokens,protection,new JdbcIdentityRateLimiter(source,key(75)),smtp,HTTP_ORIGIN,clock,()->(i,p,o,a,n,e)->false,true,"TEST");
            var applications=new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationWorkflow(source,services.customers(),services.businessContacts(),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key(76)),"v1"),disk,new com.elogistic.backend.platform.internal.infrastructure.ClamAvEvidenceScreening("127.0.0.1",daemon.port(),2000),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedGovernancePayloads(Map.of("e1",key(72)),"e1"),clock,true,"TEST");
            var http=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.elogistic.backend.workflow.internal.web.MerchantApplicationHttp(services.customers(),services.requests(),applications),new com.elogistic.backend.workflow.internal.web.MerchantApplicationWorkflowHttp(services.customers(),services.requests(),applications),new com.elogistic.backend.workflow.internal.web.CustomerProfileHttp(services.customers(),services.requests(),new com.elogistic.backend.merchant.internal.application.CustomerProfileApplication(services.customers(),new com.elogistic.backend.merchant.internal.infrastructure.JdbcCustomerProfileStore(source),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key(76)),"v1"),clock),applications)).setControllerAdvice(new com.elogistic.backend.workflow.internal.web.OnboardingHttpErrors()).addFilters(services.guard()).build();
            var owner=login(http,account,password);var foreign=login(http,other,password);
            var created=http.perform(post(owner,"/api/v1/merchant-applications",JSON.writeValueAsString(Map.of("entityType","COMPANY","jurisdiction","SYNTHETIC-J1","registeredName","Synthetic HTTP business","legalIdentifier","HTTP-REG-1")),UUID.randomUUID())).andReturn().getResponse();assertEquals(201,created.getStatus());
            String app=JSON.readTree(created.getContentAsString()).get("id").asString(),path="/api/v1/merchant-applications/"+app;
            var requirementsRequest=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path+"/requirements").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access());
            var requirements=http.perform(requirementsRequest).andReturn().getResponse();assertEquals(200,requirements.getStatus());assertEquals("no-store",requirements.getHeader("Cache-Control"));
            var requirementsBody=JSON.readTree(requirements.getContentAsString());assertEquals(Set.of("applicationId","policyId","policyVersion","consentVersion","consentStatements","requiredEvidence"),requirementsBody.propertyNames());assertEquals(app,requirementsBody.get("applicationId").asString());assertEquals("1",requirementsBody.get("policyVersion").asString());assertEquals("Synthetic accuracy statement",requirementsBody.get("consentStatements").get("accuracy").asString());
            assertEquals(404,http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path+"/requirements").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+foreign.access())).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(requirementsRequest.queryParam("policyId",UUID.randomUUID().toString())).andReturn().getResponse().getStatus());
            String incomplete="/api/v1/merchant-applications/"+draft(http,owner)+"/requirements";
            assertEquals(422,http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(incomplete).secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access())).andReturn().getResponse().getStatus());
            var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png);byte[] bytes=png.toByteArray();UUID key=UUID.randomUUID();
            assertEquals(401,http.perform(upload(owner,path+"/evidence",bytes,key).header("X-CSRF-Token","invalid-challenge")).andReturn().getResponse().getStatus());
            assertEquals(404,http.perform(upload(foreign,path+"/evidence",bytes,key)).andReturn().getResponse().getStatus());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application_evidence_reservation",Integer.class));
            var result=http.perform(upload(owner,path+"/evidence",bytes,key)).andReturn().getResponse();assertEquals(201,result.getStatus());var metadata=JSON.readTree(result.getContentAsString());
            assertEquals(Set.of("id","applicationId","purpose","state","mime","bytes","sha256","version","screenedAt"),metadata.propertyNames());assertEquals("CLEAN",metadata.get("state").asString());assertFalse(result.getContentAsString().contains("blob_key"));assertFalse(result.getContentAsString().contains("synthetic.png"));
            assertEquals(result.getContentAsString(),http.perform(upload(owner,path+"/evidence",bytes,key)).andReturn().getResponse().getContentAsString());assertEquals(1,daemon.requests.get());
            String evidence=path+"/evidence/"+metadata.get("id").asString();
            var binary=http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(evidence+"/content").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access())).andReturn().getResponse();assertEquals(200,binary.getStatus());assertArrayEquals(bytes,binary.getContentAsByteArray());assertEquals("image/png",binary.getContentType());assertEquals("no-store",binary.getHeader("Cache-Control"));assertTrue(binary.getHeader("Content-Disposition").startsWith("attachment"));
            assertEquals(404,http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(evidence+"/content").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+foreign.access())).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(upload(owner,path+"/evidence",bytes,UUID.randomUUID()).queryParam("owner",other.toString())).andReturn().getResponse().getStatus());
            assertEquals(422,http.perform(upload(owner,path+"/evidence","not an image".getBytes(StandardCharsets.US_ASCII),UUID.randomUUID())).andReturn().getResponse().getStatus());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_evidence",Integer.class));
            assertEquals(428,http.perform(post(owner,path+"/submit","{\"organizationDisplayName\":\"Synthetic\"}",UUID.randomUUID())).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(post(owner,path+"/submit","{\"organizationDisplayName\":\"Synthetic\",\"state\":\"APPROVED\"}",UUID.randomUUID()).header("If-Match","\"v1\"")).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(post(owner,path+"/submit","{\"organizationDisplayName\":\"A\",\"organizationDisplayName\":\"B\"}",UUID.randomUUID()).header("If-Match","\"v1\"")).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(post(owner,path+"/submit","{\"organizationDisplayName\":\"Synthetic\"}",UUID.randomUUID()).header("If-Match","\"v1\"")).andReturn().getResponse().getStatus());
            assertEquals("DRAFT",jdbc.queryForObject("SELECT state FROM merchant.application WHERE id=?",String.class,UUID.fromString(app)));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.application_revision",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));
            UUID archiveKey=UUID.randomUUID();
            var archive=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(evidence).secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access()).header("X-CSRF-Token",owner.challenge()).header("Idempotency-Key",archiveKey.toString()).header("If-Match","\"v1\"").cookie(owner.csrf(),owner.refresh());
            assertEquals(204,http.perform(archive).andReturn().getResponse().getStatus());assertEquals(204,http.perform(archive).andReturn().getResponse().getStatus());
            assertEquals(422,http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(evidence+"/content").secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access())).andReturn().getResponse().getStatus());
            assertEquals("ARCHIVED",jdbc.queryForObject("SELECT state FROM merchant.application_evidence WHERE id=?",String.class,UUID.fromString(metadata.get("id").asString())));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM merchant.application_evidence",Integer.class));
            UUID withdrawKey=UUID.randomUUID();String reason="{\"reason\":\"Synthetic withdrawal\"}";
            var withdrawn=http.perform(post(owner,path+"/withdrawal",reason,withdrawKey).header("If-Match","\"v1\"")).andReturn().getResponse();assertEquals(200,withdrawn.getStatus());assertEquals("WITHDRAWN",JSON.readTree(withdrawn.getContentAsString()).get("state").asString());
            assertEquals(withdrawn.getContentAsString(),http.perform(post(owner,path+"/withdrawal",reason,withdrawKey).header("If-Match","\"v1\"")).andReturn().getResponse().getContentAsString());
            var receipt=http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/onboarding-receipts/WITHDRAW/"+withdrawKey).secure(true).header("Origin",HTTP_ORIGIN).header("Authorization","Bearer "+owner.access())).andReturn().getResponse();assertEquals(200,receipt.getStatus());
            assertEquals(Set.of("operationId","targetId","outcome","version","activation"),JSON.readTree(receipt.getContentAsString()).propertyNames());assertEquals(2,JSON.readTree(receipt.getContentAsString()).get("version").asLong());
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM merchant.organization",Integer.class));

        }
    }
    public void httpOwnedDraftAndRealProof(UUID other,String password)throws Exception{
        var generator=java.security.KeyPairGenerator.getInstance("RSA");generator.initialize(3072);var pair=generator.generateKeyPair();
        var tokens=new com.elogistic.backend.identity.internal.web.ProductionAccessTokens("https://customer-identity.example.invalid","customer-browser","contact-test",(java.security.interfaces.RSAPrivateKey)pair.getPrivate(),Map.of("contact-test",(java.security.interfaces.RSAPublicKey)pair.getPublic()),clock);
        try(var inbox=new CustomerSmtpTestInbox(1,false,false,250,null)){
            var smtp=new SmtpCustomerDelivery(new SmtpCustomerDelivery.Settings("127.0.0.1",inbox.port(),SmtpCustomerDelivery.Mode.TEST_LOOPBACK,null,null,"delivery@example.invalid",Set.of("example.invalid"),HTTP_ORIGIN,true),clock);
            JdbcCustomerBusinessContacts.requireSchema(source);
            var services=com.elogistic.backend.identity.internal.ProductionCustomerComposition.create(source,tokens,protection,new JdbcIdentityRateLimiter(source,key(75)),smtp,HTTP_ORIGIN,clock,()->(i,p,o,a,n,e)->false,true,"TEST");
            var applications=new com.elogistic.backend.merchant.internal.application.MerchantApplicationService(services.customers(),new com.elogistic.backend.merchant.internal.infrastructure.JdbcMerchantApplicationStore(source),new com.elogistic.backend.merchant.internal.infrastructure.ProtectedMerchantFingerprints(Map.of("v1",key(76)),"v1"),clock);
            var http=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.elogistic.backend.workflow.internal.web.CustomerBusinessContactHttp(services.customers(),services.requests(),applications,services.businessContacts()),new com.elogistic.backend.workflow.internal.web.MerchantApplicationHttp(services.customers(),services.requests(),applications)).setControllerAdvice(new com.elogistic.backend.workflow.internal.web.OnboardingHttpErrors()).addFilters(services.guard()).build();
            var browser=login(http,account,password);var foreign=login(http,other,password);UUID app=draft(http,browser),foreignApp=draft(http,foreign),key=UUID.randomUUID();
            String path="/api/v1/customer/business-contact-proofs",body=JSON.writeValueAsString(Map.of("applicationId",app,"email",CONTACT));
            assertEquals(401,http.perform(post(browser,path,body,key).header("Origin","https://foreign.example.invalid")).andReturn().getResponse().getStatus());
            assertEquals(400,http.perform(post(browser,path,body,key).queryParam("proof","must-not-be-in-query")).andReturn().getResponse().getStatus());
            assertEquals(404,http.perform(post(browser,path,JSON.writeValueAsString(Map.of("applicationId",foreignApp,"email",CONTACT)),key)).andReturn().getResponse().getStatus());
            assertEquals(0,count("customer_proof"));
            var accepted=http.perform(post(browser,path,body,key)).andReturn().getResponse();assertEquals(202,accepted.getStatus());
            var receipt=JSON.readTree(accepted.getContentAsString());assertEquals(Set.of("requestId","accepted"),receipt.propertyNames());UUID proof=UUID.fromString(receipt.get("requestId").asString());
            assertTrue(services.delivery().runOne());byte[] token=mailToken(inbox.messages().getFirst(),app,proof);
            try{
                String confirmation=JSON.writeValueAsString(Map.of("applicationId",app,"proofId",proof,"proof",new String(token,StandardCharsets.US_ASCII)));UUID confirmKey=UUID.randomUUID();String confirmPath="/api/v1/customer/business-contact-confirmations";
                assertEquals(404,http.perform(post(foreign,confirmPath,confirmation,confirmKey)).andReturn().getResponse().getStatus());
                var confirmed=http.perform(post(browser,confirmPath,confirmation,confirmKey)).andReturn().getResponse();assertEquals(200,confirmed.getStatus());
                assertEquals(Set.of("id","applicationId","verifiedAt","expiresAt"),JSON.readTree(confirmed.getContentAsString()).propertyNames());assertFalse(confirmed.getContentAsString().contains(CONTACT));assertFalse(confirmed.getContentAsString().contains(new String(token,StandardCharsets.US_ASCII)));
                assertEquals(confirmed.getContentAsString(),http.perform(post(browser,confirmPath,confirmation,confirmKey)).andReturn().getResponse().getContentAsString());
                assertEquals(200,http.perform(post(browser,confirmPath,confirmation,confirmKey)).andReturn().getResponse().getStatus());
                assertEquals(1,count("business_contact_attestation"));assertEquals(2,count("governance_proof_receipt"));
                assertEquals(400,http.perform(post(browser,path,"{\"applicationId\":\""+app+"\",\"email\":\"a@example.invalid\",\"email\":\"b@example.invalid\"}",UUID.randomUUID())).andReturn().getResponse().getStatus());
                assertEquals(401,http.perform(post(browser,path,body,UUID.randomUUID()).header("X-CSRF-Token","invalid-challenge")).andReturn().getResponse().getStatus());
                for(int n=0;n<2;n++)assertEquals(202,http.perform(post(browser,path,body,UUID.randomUUID())).andReturn().getResponse().getStatus());
                var limited=http.perform(post(browser,path,body,UUID.randomUUID())).andReturn().getResponse();assertEquals(429,limited.getStatus());assertNotNull(limited.getHeader("Retry-After"));
                assertEquals("RATE_LIMITED",JSON.readTree(limited.getContentAsString()).get("code").asString());

            }finally{Arrays.fill(token,(byte)0);}
        }
    }
    public void contextReplayResendAndLimitsFailClosed()throws Exception{
        UUID app=UUID.randomUUID(),key=UUID.randomUUID();var first=run(c->contacts.request(c,app,CONTACT,key));
        assertEquals(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT,assertThrows(IdentityCommandFailure.class,()->run(c->contacts.request(c,UUID.randomUUID(),CONTACT,key))).code());
        assertThrows(IdentityCommandFailure.class,()->run(c->recompose(false,"TEST").request(c,app,CONTACT,UUID.randomUUID())));
        assertThrows(IdentityCommandFailure.class,()->run(c->recompose(true,"PRODUCTION").request(c,app,CONTACT,UUID.randomUUID())));
        var captured=run(c->c);assertThrows(IdentityCommandFailure.class,()->contacts.request(captured,app,CONTACT,UUID.randomUUID()));
        var second=run(c->contacts.request(c,app,CONTACT,UUID.randomUUID()));
        assertNotEquals(first.requestId(),second.requestId());
        assertNotNull(jdbc.queryForObject("SELECT revoked_at FROM identity.customer_proof WHERE id=?",java.sql.Timestamp.class,first.requestId()));
        assertEquals("EXPIRED",jdbc.queryForObject("SELECT status FROM identity.customer_delivery WHERE proof_id=?",String.class,first.requestId()));
        run(c->contacts.request(c,app,CONTACT,UUID.randomUUID()));
        assertThrows(CustomerBusinessContacts.Limited.class,()->run(c->contacts.request(c,app,CONTACT,UUID.randomUUID())));
        assertEquals(3,count("customer_proof"));assertEquals(0,count("business_contact_attestation"));
        assertEquals(first,run(c->contacts.request(c,app,CONTACT,key)),"RECEIPT_REPLAY_MUST_NOT_RENEW_OR_RESEND");
    }
}
