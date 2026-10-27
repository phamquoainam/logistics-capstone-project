"""Mutation controls for the Owner-approved Spec003 refinements."""
import copy
import unittest
import validate_spec003 as v

class Spec003PolicyTests(unittest.TestCase):
    def setUp(self):self.data=copy.deepcopy(v.load())
    def mutate(self,group,key,value):
        self.data[0][group][key]=value
        self.assertTrue(v.errors(*self.data))
    def test_current_package(self):self.assertEqual([],v.errors(*self.data))
    def test_profile_not_merchant_tenant(self):self.mutate('profile','requires_org',True)
    def test_profile_not_membership_dependent(self):self.mutate('profile','requires_membership',True)
    def test_profile_no_security_master(self):self.mutate('profile','can_change_security',True)
    def test_phone_not_authentication(self):self.mutate('customer','phone_authentication',True)
    def test_registration_cannot_grant_owner(self):self.mutate('customer','public_grants',['OWNER'])
    def test_customer_default_personal(self):self.mutate('customer','default_context','MERCHANT')
    def test_email_verification_required(self):self.mutate('customer','login_identifier','UNVERIFIED_PHONE')
    def test_no_cross_admission_via_shared_refresh_tables(self):self.mutate('customer','session_storage','SHARED_WITH_LEGACY_SYSTEM_LOOKUP')
    def test_customer_token_not_privileged_system_token(self):self.mutate('customer','token_trust','SAME_ACCEPTED_SYSTEM_AUDIENCE')
    def test_identifier_index_not_second_verification_master(self):self.mutate('customer','identifier_authority','INDEPENDENT_IDENTIFIER_VERIFIED_FLAG')
    def test_legacy_link_cannot_replace_security_contact(self):self.mutate('customer','legacy_link_email','ANY_NEW_EMAIL_PROOF')
    def test_recovery_deadline(self):self.mutate('customer','recovery_minutes',1440)
    def test_invitation_cannot_reuse_identity(self):self.mutate('invitation','accept_identity_invitation_reuse',True)
    def test_inviter_revoke_invalidates_pending(self):self.mutate('invitation','inviter_change','KEEP_ACTIVE')
    def test_invite_resend_revokes_proof(self):self.mutate('invitation','resend','KEEP_PRIOR_PROOF')
    def test_invite_policy_change_invalidates(self):self.mutate('invitation','policy_change','KEEP_ACTIVE')
    def test_revocation_before_mutation_denies(self):self.mutate('transaction','revoke_first','ALLOW_UNTIL_JWT_EXPIRES')
    def test_prior_commit_not_retroactively_rolled_back(self):self.mutate('transaction','mutation_first','ROLLBACK')
    def test_existing_session_transaction_ownership(self):self.mutate('transaction','ambient_transaction','ACCEPT')
    def test_no_remote_io_in_atomic_activation(self):self.mutate('transaction','external_io',True)
    def test_approval_cannot_be_partial_active(self):self.mutate('transaction','activation','PARTIAL_ACTIVE')
    def test_no_self_review(self):self.mutate('application','self_review',True)
    def test_resubmit_new_revision(self):self.mutate('application','resubmit','UPDATE_OLD_SNAPSHOT')
    def test_owner_not_client_flag(self):self.mutate('application','owner_from','REQUEST_OWNER_FLAG')
    def test_last_owner_protected(self):self.mutate('application','last_owner','OPTIONAL')
    def test_recovery_no_single_admin_bypass(self):self.mutate('application','recovery','IDENTITY_ADMIN')
    def test_closure_missing_is_not_zero(self):self.mutate('application','closure_missing_clearance','CLOSE')
    def test_purge_not_auto_enabled(self):self.mutate('privacy','purge_enabled',True)
    def test_retention_holds_not_omitted(self):self.mutate('privacy','hold_exemptions',[])
    def test_sandbox_retention_not_legal_claim(self):self.mutate('privacy','retention_scope','PRODUCTION_LEGAL')
    def test_quarantine_unavailable_fails_closed(self):self.mutate('privacy','scanner_unavailable','ALLOW_REVIEW')
    def test_smtp_acceptance_not_delivery(self):self.mutate('privacy','provider_accept_is_delivered',True)
    def test_no_internet_rollout(self):self.mutate('privacy','internet_rollout',True)
    def test_no_fake_ready(self):self.mutate('review_gates','candidate_contract_acceptance','ACCEPTED')
    def test_cannot_drop_owned_core(self):
        self.data[3].pop();self.assertTrue(v.errors(*self.data))
    def test_bc045_stays_deny(self):
        next(r for r in self.data[3] if r['bc_id']=='BC-045')['positive_ac']='ALLOW';self.assertTrue(v.errors(*self.data))
    def test_bc159_not_fake_catalog(self):
        next(r for r in self.data[3] if r['bc_id']=='BC-159')['dependencies']='LOCAL_FAKE';self.assertTrue(v.errors(*self.data))
    def test_source_nfr_cannot_be_omitted(self):
        self.data[3][0]['source_refs']='FR-A02;UC:AD02';self.assertTrue(v.errors(*self.data))
    def test_source_nfr_cannot_be_invented(self):
        self.data[3][0]['source_refs']+=';NFR-Z99';self.assertTrue(v.errors(*self.data))
    def test_cannot_drop_revoke_race(self):
        self.data[2]['scenarios']=[s for s in self.data[2]['scenarios'] if s['id']!='S003-REVOKE-FIRST'];self.assertTrue(v.errors(*self.data))
    def test_no_fabricated_runtime_pass(self):
        self.data[2]['scenarios'][0]['evidence']='PASS';self.assertTrue(v.errors(*self.data))

if __name__=='__main__':unittest.main()
