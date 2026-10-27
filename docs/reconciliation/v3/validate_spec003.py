"""Read-only Spec003 policy/traceability gates; no runtime acceptance."""
import csv
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
PACKAGE=ROOT/'specs/003-sender-merchant-onboarding/contracts'
BCS={'BC-007','BC-008','BC-020','BC-032','BC-045','BC-132','BC-133','BC-134','BC-135','BC-157','BC-158','BC-159','BC-166','BC-167'}
DECISIONS={'IAM-CUSTOMER-01','IAM-CONTEXT-02','IAM-INVITE-03','POLICY-003-01','DATA-01','PRIVACY-01'}
REQUIRED_SCENARIOS=set('S003-PROFILE S003-PROFILE-INJECTION S003-REVOKE-FIRST S003-MUTATION-FIRST S003-INVITER-REVOKE S003-INVITE-RACE S003-LAST-OWNER S003-OWNER-RECOVERY S003-APPROVAL-RACE S003-AUDIT-FAIL S003-ACTIVATION-FAIL S003-CLOSURE S003-CATALOG-MISSING S003-QUOTA-RACE S003-MIME S003-SCANNER S003-DOWNLOAD S003-DELIVERY S003-RETENTION S003-PG S003-ABUSE S003-PRIVACY S003-PHONE S003-RECOVERY S003-ENUMERATION'.split())

def load():
    read=lambda name:json.loads((PACKAGE/name).read_text(encoding='utf-8'))
    with (PACKAGE/'coverage.csv').open(encoding='utf-8',newline='') as f:coverage=list(csv.DictReader(f))
    return read('policy.json'),read('operation-index.json'),read('fixtures.json'),coverage

def errors(policy,operations,fixtures,coverage):
    problems=[]
    def check(ok,message):
        if not ok:problems.append(message)
    check(policy.get('status')=='CANDIDATE_FOR_OWNER_REVIEW' and policy.get('runtime_evidence')=='NOT_IMPLEMENTED','no implicit technical/runtime acceptance')
    check(policy.get('decisions')==dict.fromkeys(DECISIONS,'OWNER_APPROVED_WITH_REFINEMENTS'),'six approved policy dispositions')
    expected={
      'customer':dict(default_context='PERSONAL',login_identifier='VERIFIED_EMAIL',phone_authentication=False,public_grants=[],verification_hours=24,recovery_minutes=15,security_contact_reauthentication=True,security_master='identity'),
      'profile':dict(authority='customer_profile',physical_module_candidate='merchant',requires_org=False,requires_membership=False,account_reference='IMMUTABLE_IDENTITY_ACCOUNT_ID',can_change_security=False),
      'invitation':dict(hours=72,single_use=True,recipient_bound=True,purpose='MERCHANT_MEMBERSHIP',accept_identity_invitation_reuse=False,inviter_change='INVALIDATE_PENDING',policy_change='INVALIDATE_PENDING',resend='REVOKE_PRIOR_PROOFS'),
      'transaction':dict(owner='NEW_IDENTITY_CUSTOMER_ADMISSION',ambient_transaction='REJECT',database='SAME_POSTGRESQL_DATASOURCE',isolation='READ_COMMITTED',decision_point='FINAL_LOCKED_AUTHORIZATION_BEFORE_COMMIT',revoke_first='DENY_MUTATION',mutation_first='PRESERVE_COMMIT',external_io=False,activation='ATOMIC_OR_NO_ACTIVE_AUTHORITY'),
      'application':dict(entity_types=['COMPANY','HOUSEHOLD_BUSINESS'],resubmit='NEW_IMMUTABLE_SUBMITTED_REVISION',review='MANUAL_SANDBOX',self_review=False,owner_from='LOCKED_VERIFIED_APPLICANT',last_owner='ATOMIC_ELIGIBLE_REPLACEMENT_REQUIRED',recovery='TWO_INDEPENDENT_SCOPED_BUSINESS_ADMINS',closure_missing_clearance='KEEP_CLOSURE_REQUESTED'),
      'privacy':dict(max_file_bytes=5242880,max_files=5,media_types=['application/pdf','image/png','image/jpeg'],storage='PRIVATE_QUARANTINE',scanner_unavailable='DENY_ACCESS',purge_enabled=False,draft_days=30,terminal_evidence_days=90,audit_days=365,retention_scope='CAPSTONE_SANDBOX_ONLY',provider_accept_is_delivered=False,internet_rollout=False)}
    for group,fields in expected.items():
        for key,value in fields.items():check(policy.get(group,{}).get(key)==value,group+'.'+key)
    check(policy.get('customer',{}).get('session_storage')=='IDENTITY_OWNED_CUSTOMER_TABLES_SEPARATE_FROM_SYSTEM' and policy.get('customer',{}).get('token_trust')=='DISTINCT_CUSTOMER_ISSUER_AUDIENCE_KEYS','no customer/SYSTEM token or refresh confusion')
    check(policy.get('transaction',{}).get('lock_order')==['IDENTITY_ADMINISTRATION','ACCOUNT_UUID_SORTED','SESSION_UUID_SORTED','MERCHANT_CONTROL','ORGANIZATION_UUID_SORTED','BUSINESS_KEY','RESOURCE_UUID_SORTED'],'transaction.lock_order')
    check(set(policy.get('privacy',{}).get('hold_exemptions',[]))=={'ACTIVE_REVIEW','APPEAL','DISPUTE','LEGAL_HOLD','ADMIN_HOLD','ACTIVE_ORG_REFERENCE','OPEN_OBLIGATION','UNRESOLVED_RECOVERY'},'retention hold/reference safeguards')
    gates=policy.get('review_gates',{})
    check(gates.get('candidate_contract_acceptance')=='PENDING_OWNER_REVIEW' and gates.get('implementation_authorization')=='NOT_AUTHORIZED' and gates.get('spec_acceptance')=='NOT_ACCEPTED','honest DoR/authorization gates')
    deps=policy.get('dependencies',{})
    check(set(deps)=={'CAT-004','POL-005','CLOSE-OWNERS'},'minimum owner dependencies')
    for key,owner in [('CAT-004','L004'),('POL-005','L005'),('CLOSE-OWNERS','L006;L018;L005;L021')]:
        check(deps.get(key,{}).get('owner')==owner and bool(deps.get(key,{}).get('fields')),'dependency authority '+key)
    roles=policy.get('roles',{})
    check(roles.get('IDENTITY_ADMIN_ONLY',{}).get('permissions')==[],'Identity Admin cannot gain business authority')
    check(not {'org.legal.write','ownership.transfer','ownership.recover','application.review'} & set(roles.get('MANAGER',{}).get('permissions',[])),'Manager delegation bounds')
    states=policy.get('states',{});transitions=policy.get('transitions',{})
    for kind,edges in transitions.items():
        check(kind in states and all(a in states[kind] and b in states[kind] for a,b,_ in edges),'known state transitions '+kind)
    check(['UNDER_REVIEW','APPROVED','approveAtomicActivation'] in transitions.get('application',[]),'atomic approval transition')
    check(not any(b=='CLOSED' and action!='closeWithClearance' for _,b,action in transitions.get('organization',[])),'closure cannot bypass clearance')
    check(not any(a=='REVOKED' and b=='ACTIVE' for a,b,_ in transitions.get('membership',[])),'revoked membership cannot revive')
    check(not any(a=='QUEUED' and b=='DELIVERED' for a,b,_ in transitions.get('delivery',[])),'queued is not delivered')
    ids={o['operation_id'] for o in operations};schemas=set(json.loads((PACKAGE/'openapi.candidate.json').read_text(encoding='utf-8'))['components']['schemas'])
    check(len(ids)==len(operations)==75,'exact operation inventory')
    check(len(coverage)==14 and {r['bc_id'] for r in coverage}==BCS,'complete13CORE+DENY045 coverage')
    scenario_ids={s['id'] for s in fixtures.get('scenarios',[])}
    check(fixtures.get('status')=='DECLARATIVE_NOT_RUNTIME_EXECUTED' and REQUIRED_SCENARIOS<=scenario_ids,'required negative/concurrency/failure scenarios')
    check(len(scenario_ids)==len(fixtures.get('scenarios',[])),'unique scenario IDs')
    for scenario in fixtures.get('scenarios',[]):
        check(scenario.get('operation') in ids and all(scenario.get(k) for k in ['given','when','expected_effect']) and scenario.get('evidence')=='PLANNED_NOT_EXECUTED','honest executable acceptance design '+scenario.get('id','?'))
    for row in coverage:
        bc=row['bc_id'];check(all(row.values()),'nonempty coverage '+bc)
        check(row['requirement']=='REQ-V3-'+bc and row['evidence_status']=='PLANNED_NOT_EXECUTED','requirement/status '+bc)
        check(set(row['operations'].split(';'))<=ids and set(row['schemas'].split(';'))<=schemas,'operation/schema trace '+bc)
        check(set(row['scenarios'].split(';'))<=scenario_ids,'scenario trace '+bc)
        check(row['classification']==('MANDATORY_DENY' if bc=='BC-045' else 'CORE'),'deny classification '+bc)
        check(row['positive_ac']==('DENY_ONLY' if bc=='BC-045' else 'AC-'+bc+'-SUCCESS'),'positive/deny AC '+bc)
        check(all('T-'+bc+'-'+layer in row['tasks'].split(';') for layer in ['CONTRACT','DB','BE','UI','VERIFY']),'five-layer tasks '+bc)
        refs=row['source_refs']
        check({'NFR-S01','NFR-S02','NFR-S03','NFR-S04','NFR-M01','NFR-M02'}<=set(re.findall(r'NFR-[A-Z]\d{2}',refs)) and bool(re.search(r'FR-[A-Z]\d{2}',refs)) and 'UC:' in refs,'FR/BR/NFR/UC source trace '+bc)
        nfr_source=(ROOT/'docs/quality/non-functional-requirements.md').read_text(encoding='utf-8')
        check(all(code in nfr_source for code in re.findall(r'NFR-[A-Z]\d{2}',refs)),'unknown source NFR '+bc)
    if coverage:
        check(set().union(*(set(r['operations'].split(';')) for r in coverage))==ids,'unmapped operations')
        check(set().union(*(set(r['scenarios'].split(';')) for r in coverage))==scenario_ids,'unmapped scenarios')
    bc159=next((r for r in coverage if r['bc_id']=='BC-159'),{})
    check({'CAT-004','POL-005'}<=set(bc159.get('dependencies','').split(';')),'BC159 minimum owner dependencies')
    return sorted(set(problems))

if __name__=='__main__':
    result=errors(*load());print(json.dumps({'errors':result,'policy':'OWNER_APPROVED_WITH_REFINEMENTS','technical_contract':'CANDIDATE_FOR_OWNER_REVIEW','runtime':'NOT_IMPLEMENTED'},indent=2));raise SystemExit(bool(result))
