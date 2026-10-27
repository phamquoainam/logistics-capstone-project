"""Canonical docs-only gates; no application runtime/network/migration/writes.

Historical protection-baseline.json describes retired worktrees, not live state.
Freeze accepted source/CI/contracts/001/002 against the verified merge baseline.
"""
from __future__ import annotations
import csv
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'docs/reconciliation/v3'
BASELINE = 'eeb7833ab678240581c407ccd167d7e61ea873c6'
DEFER = {'BC-006', 'BC-040', 'BC-045', 'BC-046', 'BC-085'}
NEGATIVE = DEFER | {'BC-243', 'BC-252', 'BC-261', 'BC-280', 'BC-354', 'BC-355', 'BC-362'}
DOMAINS = {'identity','merchant','orders','pricing','warehouse','dispatch','finance','routing','notification','platform'}
AB_ROLES = {'R01','R18','R10','R02','R19','R03','R20','R11','R21','R22'}
PROTECTED = ('app/','contracts/','packages/','infra/','scripts/','.github/','specs/001-platform-foundation/','specs/002-identity-access/','docs/architecture/')
ID_PATTERN = re.compile(r'\b(?:REQ-\d{3}-\d{2}|AC-\d{3}-\d{2}|T-\d{3}-\d{2}(?:-[A-Z0-9]+)*|DEMO-(?:T\d+|\d+)|D1-\d{2}(?:-[A-Z0-9]+)*|ADR-\d{4}|FR-[A-Z]+\d{2}|BR-\d{2}|NFR-[A-Z]\d{2}|AC-\d{2}|DEC-\d{2})\b')

def git(*args):
    return subprocess.check_output(['git','-C',str(ROOT),*args],stderr=subprocess.PIPE).decode('utf-8-sig')

def csv_at(name):
    with (ROOT/name).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f))

def read_csv(name):return csv_at('docs/reconciliation/v3/'+name)
def body(name):return (ROOT/name).read_text(encoding='utf-8-sig')
def encoding_errors(text):return '\ufffd' in text or bool(re.search(r'[\w]\?[\w]|\?\?',text))

# Stable resource IDs and authority families, independent of prose or role labels.
RESOURCE_FAMILIES = {
    'identity_security': {'identity.account','identity.credentials','identity.verification','identity.security_contact','identity.session'},
    'retail_business': {'retail_sender.business_profile'},
    'merchant_business': {'merchant.application','merchant.organization','merchant.business_profile','merchant.membership','merchant.delegation'},
    'carrier_network': {'carrier.station','carrier.hub'},
}
FAMILY_AUTHORITIES = {'identity_security':'identity','merchant_business':'merchant','carrier_network':'dispatch'}

def ownership_errors(policy,declarations):
    """Return errors AND explicit unresolved decisions; docs consistency is not DoR."""
    errors=[];unresolved=set();resources={}
    if policy.get('schema_version')!=1:errors.append('ownership policy schema')
    if policy.get('orchestrators')!={'spec003':{'canonical_spec':'L003','grants_data_authority':False}}:
        errors.append('orchestration is not data authority')
    if policy.get('capability_authorities')!={
        'BC-134':{'orchestrator':'spec003','authority_resource':'identity.account'},
        'BC-135':{'orchestrator':'spec003','authority_resource':'retail_sender.business_profile'},
    }:errors.append('capability authority policy coverage')
    for resource in policy.get('resources',[]):
        rid=resource.get('resource_id');family=resource.get('family')
        if rid in resources:errors.append('duplicate policy resource '+str(rid))
        resources[rid]=resource
        if family not in RESOURCE_FAMILIES or rid not in RESOURCE_FAMILIES.get(family,set()):
            errors.append('undeclared resource family '+str(rid));continue
        owner=resource.get('authoritative_owner');resolution=resource.get('resolution')
        if family=='retail_business':
            if owner is not None or resolution!='UNRESOLVED' or resource.get('decision')!='DATA-01' or resource.get('candidate_owner')!='merchant':
                errors.append('unapproved Retail profile authority '+str(rid))
            unresolved.add(str(rid)+':DATA-01')
        elif owner!=FAMILY_AUTHORITIES[family] or resolution!='DECLARED' or resource.get('decision') is not None:
            errors.append('resource authority conflict '+str(rid))
    expected=set().union(*RESOURCE_FAMILIES.values())
    if set(resources)!=expected:errors.append('ownership policy resource coverage')
    expected_scopes={'BC-134':RESOURCE_FAMILIES['identity_security'],'BC-135':{'retail_sender.business_profile','identity.security_contact'}}
    if {k:set(v) for k,v in policy.get('document_scopes',{}).items()}!=expected_scopes:
        errors.append('ownership scope coverage')
    seen=set()
    for declaration in declarations:
        scope=declaration.get('scope');seen.add(scope)
        if scope not in expected_scopes:errors.append('unknown ownership scope '+str(scope));continue
        if set(declaration)!={'scope','orchestrator','resources'} or declaration.get('orchestrator')!='spec003':
            errors.append('ownership declaration schema '+str(scope))
        entries=declaration.get('resources',[])
        if not isinstance(entries,list):errors.append('ownership resource list '+scope);continue
        ids=[r.get('resource_id') for r in entries]
        if len(ids)!=len(set(ids)) or set(ids)!=expected_scopes[scope]:errors.append('ownership declaration coverage '+scope)
        for r in entries:
            rid=r.get('resource_id');authority=resources.get(rid)
            if authority is None:errors.append('undeclared authority resource '+str(rid));continue
            if set(r)!={'resource_id','authoritative_owner','resolution','decision'}:
                errors.append('ownership binding schema '+str(rid))
            if any(r.get(k)!=authority.get(k) for k in ['authoritative_owner','resolution','decision']):
                errors.append('resource authority conflict '+str(rid))
    if seen!=set(expected_scopes):errors.append('missing ownership scope')
    return errors,sorted(unresolved)

def ownership_document_errors(policy,text):
    """Typed fenced declarations are the only owner fields in governed sections."""
    declarations=[];errors=[]
    for raw in re.findall(r'^```ownership-json\s*\n(.*?)^```\s*$',text,re.M|re.S):
        try:
            item=json.loads(raw)
            if not isinstance(item,dict):raise ValueError('object required')
            declarations.append(item)
        except (ValueError,TypeError):errors.append('invalid ownership declaration JSON')
    scopes=[d.get('scope') for d in declarations]
    if len(scopes)!=len(set(scopes)):errors.append('duplicate ownership scope')
    for scope in policy.get('document_scopes',{}):
        # Headings are stable REQ/DES identifiers, not the wording of an owner sentence.
        section=re.search(r'^#{2,3} (?:DES-|REQ-V3-)'+re.escape(scope)+r'\b.*?(?=^#{1,3} |\Z)',text,re.M|re.S)
        if section is None:errors.append('missing governed ownership section '+scope);continue
        embedded=re.findall(r'^```ownership-json\s*\n(.*?)^```\s*$',section.group(),re.M|re.S)
        if len(embedded)!=1:errors.append('missing structured owner field '+scope)
        elif embedded:
            try:
                if json.loads(embedded[0]).get('scope')!=scope:errors.append('misplaced ownership scope '+scope)
            except (ValueError,AttributeError):errors.append('invalid section ownership binding '+scope)
        # Reject a competing legacy owner field instead of letting prose override schema.
        if re.search(r'\bData owner\s*:',section.group(),re.I):errors.append('competing unstructured owner field '+scope)
    semantic,unresolved=ownership_errors(policy,declarations)
    return errors+semantic,unresolved

def source_rows():
    result=[]
    for line in body('docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md').splitlines():
        if re.match(r'^\| BC-\d{3} \|',line):
            a=[v.strip() for v in line.strip('|').split('|')]
            result.append(dict(capability_id=a[0],role_id=a[1][:3],module_id=a[2],classification=a[3],business_operation=a[4],source_guard=a[5]))
    return result

def mapping_errors(rows,cards,source):
    errors=[]
    def check(ok,msg):
        if not ok:errors.append(msg)
    check(len(rows)==371 and {r['capability_id'] for r in rows}=={f'BC-{n:03}' for n in range(1,372)},'BC count/unique/continuity')
    check({r['role_id'] for r in rows}=={f'R{n:02}' for n in range(1,34)},'role coverage')
    check({r['module_id'] for r in rows}=={f'M{n:02}' for n in range(1,19)},'module coverage')
    check(sum(r['classification']=='CORE' for r in rows)==366,'CORE count')
    check({r['capability_id'] for r in rows if r['classification']=='DEFER'}==DEFER,'exact DEFER')
    index={c['id']:c for c in cards};src={r['capability_id']:r for r in source};by_bc={r['capability_id']:r for r in rows}
    check(len(cards)==30 and set(index)=={f'L{n:03}' for n in range(1,31)},'spec count/IDs')
    for r in rows:
        key=r['capability_id'];sid=r['primary_spec']
        check(sid in index and r['primary_domain_owner'] in DOMAINS,key+' primary owner')
        check(r['mapping_status']=='CANONICAL_DOC_REVIEW_REQUIRED',key+' unauthorized mapping')
        check(r['implementation_status']=='NOT_STARTED',key+' unproved target')
        check(all(c in index for c in r['secondary_consumers'].split(';') if c),key+' orphan consumer')
        check(sid not in r['secondary_consumers'].split(';'),key+' duplicate primary/consumer')
        check(key in src,key+' missing source BC')
        if key in src:
            check(all(r[k]==src[key][k] for k in ['role_id','module_id','classification','business_operation']),key+' source mutation')
            check(src[key]['source_guard'] in r['required_permission_guard'],key+' dropped guard')
        if key in NEGATIVE:check(r['acceptance_kind']=='NEGATIVE/DENY',key+' missing denial')
        check(r['target_requirement']=='REQ-V3-'+key,key+' requirement ID')
        if sid in index:
            check(key in index[sid]['capability_ids'],key+' register mismatch')
            check(r['dependency']==';'.join(index[sid]['hard_dependencies']),key+' stale dependency')
    required={'id','name','domain_owner','purpose','excluded_scope','role_ids','capability_ids','invariants','hard_dependencies','soft_dependencies','backend_scope','frontend_scope','database_scope','contract_scope','authorization','workflow','idempotency_concurrency','audit_privacy','acceptance','test_evidence','definition_of_ready','definition_of_done','implementation_order','blockers','decision_owner','folder','readiness','wave'}
    visited=set();active=set()
    def visit(key):
        if key in active:errors.append('hard dependency cycle '+key);return
        if key in visited or key not in index:return
        active.add(key)
        for dep in index[key]['hard_dependencies']:
            check(dep in index,key+' missing dependency '+dep)
            if dep in index:
                check(index[dep]['implementation_order']<index[key]['implementation_order'],key+' invalid execution order');visit(dep)
        active.remove(key);visited.add(key)
    for c in cards:
        check(required<=c.keys(),c['id']+' missing card fields')
        if c['id']=='L003':
            check(not any('Guest chọn loại→' in str(c.get(k,'')) for k in ['workflow','invariants','acceptance','frontend_scope']),'stale account-type workflow')
        check(set(c['capability_ids'])=={r['capability_id'] for r in rows if r['primary_spec']==c['id']},c['id']+' owner register mismatch')
        if c['id'] not in {'L001','L002'}:check(c['folder']=='specs/'+c['id'][1:]+'-'+c['name'],c['id']+' canonical path')
        visit(c['id'])
    check(len({c['folder'] for c in cards})==30,'physical folder collision')
    for key,sid,domain in [('BC-009','L004','merchant'),('BC-021','L004','merchant'),('BC-168','L004','merchant'),('BC-230','L011','dispatch'),('BC-305','L011','dispatch'),('BC-329','L020','merchant'),('BC-330','L020','merchant'),('BC-331','L005','pricing'),('BC-335','L022','merchant')]:
        if key in by_bc:check((by_bc[key]['primary_spec'],by_bc[key]['primary_domain_owner'])==(sid,domain),key+' corrected ownership')
    if 'BC-133' in by_bc:check(by_bc['BC-133']['role_id']=='R18' and 'không Guest submit' in by_bc['BC-133']['required_permission_guard'],'account-first Merchant application')
    return errors

def portal_errors(coverage,supplemental,journeys,rows,register):
    errors=[];by_bc={r['capability_id']:r for r in rows};expected={r['capability_id'] for r in rows if r['role_id'] in AB_ROLES}
    if len(coverage)!=112 or {r['bc_id'] for r in coverage}!=expected:errors.append('portal BC count/coverage')
    fields={'bc_id','actor','business_function','canonical_spec','requirement','design','task','ui_channel','api','db_owner','permission','dependencies','acceptance','status','gap','spec_path','journey_ids'}
    jids={j['journey_id'] for j in journeys};jbc={b for j in journeys for b in j['bc_ids'].split(';')}
    if len(journeys)!=10 or jids!={f'J-AB-{i:02}' for i in range(1,11)}:errors.append('portal journeys')
    for r in coverage:
        if not fields<=r.keys() or not all(r.get(k) for k in fields):errors.append(r.get('bc_id','?')+' portal field')
        src=by_bc.get(r['bc_id'])
        if src and (r['canonical_spec']!=src['primary_spec'] or r['requirement']!=src['target_requirement']):errors.append(r['bc_id']+' portal owner mismatch')
        if r['bc_id'] not in jbc or r['journey_ids'] not in jids:errors.append(r['bc_id']+' portal journey orphan')
        if r['status']!='PLANNED_NOT_IMPLEMENTED_NOT_ACCEPTED':errors.append(r['bc_id']+' false portal evidence')
    core={b for b in expected if by_bc[b]['classification']=='CORE'}
    if set(register['core_bc_ids'])!=core or set(register['required_denials'])!={'BC-006','BC-085'}:errors.append('portal acceptance incomplete')
    if register['status']!='NOT_STARTED_NOT_ACCEPTED' or register['evidence']:errors.append('fabricated product acceptance')
    reqs={s['requirement'] for s in supplemental}
    if len(supplemental)!=23 or len(reqs)!=23 or set(register['supplemental_requirement_ids'])!=reqs:errors.append('supplemental coverage')
    return errors

def local_link_errors(path,text):
    errors=[]
    for target in re.findall(r'(?<!!)\[[^\]]+\]\(([^)]+)\)',text):
        if target.startswith(('https:','http:','mailto:','#')):continue
        local,_,anchor=target.partition('#');dest=(path.parent/unquote(local.strip('<>'))).resolve()
        bad=not dest.exists()
        if anchor and dest.is_file() and dest.suffix=='.md':
            text2=dest.read_text(encoding='utf-8-sig');headings={re.sub(r'[^\w\- ]','',h.lower()).replace(' ','-') for h in re.findall(r'^#{1,6} (.+)$',text2,re.M)}
            bad=bad or anchor not in headings and f'id="{anchor}"' not in text2
        if bad:errors.append(path.relative_to(ROOT).as_posix()+' -> '+target)
    return errors

def run():
    rows=read_csv('capability-crosswalk.csv');cards=json.loads(body('docs/reconciliation/v3/spec-register.json'));index={c['id']:c for c in cards}
    errors=mapping_errors(rows,cards,source_rows());coverage=csv_at('docs/portal/group-ab-coverage.csv');supp=csv_at('docs/portal/supplemental-requirements.csv');journeys=csv_at('docs/portal/journeys.csv');accept=json.loads(body('docs/portal/acceptance-register.json'))
    errors+=portal_errors(coverage,supp,journeys,rows,accept)
    policy=json.loads(body('docs/reconciliation/v3/resource-authority-policy.json'));ownership_unresolved=set()
    if policy.get('declaration_documents')!=['specs/003-sender-merchant-onboarding/design.md','specs/003-sender-merchant-onboarding/tasks.md']:
        errors.append('ownership declaration document coverage')
    for name in policy.get('declaration_documents',[]):
        semantic,unresolved=ownership_document_errors(policy,body(name))
        errors.extend(name+': '+error for error in semantic);ownership_unresolved.update(unresolved)
    if policy.get('coverage_document')!='docs/portal/group-ab-coverage.csv':errors.append('ownership coverage document')
    bindings=[]
    for scope in policy.get('document_scopes',{}):
        row=next((r for r in coverage if r['bc_id']==scope),None)
        try:bindings.append(json.loads(row['db_owner']))
        except (ValueError,TypeError,KeyError):errors.append('missing structured portal authority '+scope)
    semantic,unresolved=ownership_errors(policy,bindings);errors.extend(semantic);ownership_unresolved.update(unresolved)
    authorities={r['resource_id']:r for r in policy.get('resources',[])}
    for scope,binding in policy.get('capability_authorities',{}).items():
        row=next((r for r in rows if r['capability_id']==scope),None);authority=authorities.get(binding.get('authority_resource'))
        if row is None or authority is None or binding.get('orchestrator')!='spec003':errors.append('capability authority binding '+scope);continue
        owner=authority.get('authoritative_owner') if authority['resolution']=='DECLARED' else authority.get('candidate_owner')
        if row['primary_domain_owner']!=owner or row['primary_spec']!='L003':errors.append('capability resource authority conflict '+scope)
    tree=set(git('ls-tree','-r','--name-only',BASELINE).splitlines());changed=set(git('diff','--name-only',BASELINE).splitlines())|set(git('ls-files','--others','--exclude-standard').splitlines())
    for n in changed:
        if n.startswith(PROTECTED):errors.append('protected source changed '+n)
        if not(n.startswith(('docs/','specs/')) or n=='AGENTS.md'):errors.append('non-doc change '+n)
    legacy=read_csv('legacy-crosswalk.csv');oldlegacy=list(csv.DictReader(git('show',BASELINE+':docs/reconciliation/v3/legacy-crosswalk.csv').splitlines()));identities={(r['path'],r['line'],r['legacy_id']):r for r in legacy}
    if len(legacy)!=len(oldlegacy) or len(identities)!=len(legacy):errors.append('legacy record count/duplicates')
    for r in oldlegacy:
        current=identities.get((r['path'],r['line'],r['legacy_id']))
        if current is None or current['source_statement']!=r['source_statement']:errors.append('legacy ID/statement dropped '+r['path']+' '+r['legacy_id'])
    overlay=read_csv('retirement-id-supplement.csv')
    legacy_keys={(r['path'],r['legacy_id']) for r in legacy+overlay};retirement=read_csv('retirement.csv');promotion=json.loads(body('docs/reconciliation/v3/planning-promotion-history.json'))
    for r in overlay:
        original=git('show',BASELINE+':'+r['path']).splitlines()
        if original[int(r['line'])-1]!=r['source_statement'] or r['legacy_id'] not in r['source_statement']:
            errors.append('overlay provenance mismatch '+r['path']+' '+r['legacy_id'])
    for r in retirement:
        if (ROOT/r['path']).exists():errors.append('retired duplicate active '+r['path'])
        raw=subprocess.check_output(['git','-C',str(ROOT),'show',BASELINE+':'+r['path']])
        if hashlib.sha256(raw).hexdigest()!=r['git_blob_sha256']:errors.append('retirement history digest '+r['path'])
        for ident in set(ID_PATTERN.findall(raw.decode('utf-8-sig'))):
            if (r['path'],ident) not in legacy_keys:errors.append('legacy ID dropped '+r['path']+' '+ident)
    for p in promotion:
        if (ROOT/p['path']).exists():errors.append('planning duplicate active '+p['path'])
        raw=subprocess.check_output(['git','-C',str(ROOT),'show',BASELINE+':'+p['path']])
        if hashlib.sha256(raw).hexdigest()!=p['git_blob_sha256']:errors.append('planning history digest '+p['path'])
        if not(ROOT/p['successor']).exists():errors.append('promotion successor missing '+p['path'])
    deleted={n for n in changed if n.startswith('specs/') and not(ROOT/n).exists()}
    if deleted!={r['path'] for r in retirement}|{p['path'] for p in promotion}:errors.append('unrecorded spec retirement')
    docs={}
    for c in cards:
        for fn in ['spec.md','design.md','tasks.md']:
            p=ROOT/c['folder']/fn
            if not p.is_file():errors.append('missing triple '+p.relative_to(ROOT).as_posix())
            elif c['id'] not in {'L001','L002'}:docs[(c['id'],fn)]=p.read_text(encoding='utf-8-sig')
    delta=body('docs/reconciliation/v3/identity-target-delta.md')
    for r in rows:
        sid=r['primary_spec'];req=r['target_requirement'];bc=r['capability_id']
        spec=delta if sid=='L002' else docs.get((sid,'spec.md'),'');design=delta if sid=='L002' else docs.get((sid,'design.md'),'');tasks=delta if sid=='L002' else docs.get((sid,'tasks.md'),'')
        if req not in spec or 'AC-'+bc not in spec:errors.append('orphan target requirement/AC '+bc)
        if 'DES-'+bc not in design:errors.append('orphan design '+bc)
        for layer in ['CONTRACT','DB','BE','UI','VERIFY']:
            if 'T-'+bc+'-'+layer not in tasks:errors.append('orphan task '+bc+' '+layer)
        for other in cards[2:]:
            if other['id']!=sid and re.search(r'^### '+re.escape(req)+r'\b',docs.get((other['id'],'spec.md'),''),re.M):errors.append('duplicate owning requirement '+bc)
    for s in supp:
        sid=s['canonical_spec'];spec=delta if sid=='L002' else docs[(sid,'spec.md')]
        if s['requirement'] not in spec:errors.append('orphan supplemental requirement '+s['requirement'])
    oldlinks=set();checked=0
    for name in sorted(changed):
        p=ROOT/name
        if not p.is_file() or p.suffix not in {'.md','.json','.csv','.py'}:continue
        text=p.read_text(encoding='utf-8-sig')
        if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|sk-proj-[A-Za-z0-9_-]{30,}',text):errors.append('possible secret material '+name)
        if p.suffix=='.md':
            checked+=1
            if encoding_errors(text):errors.append('corrupt UTF-8 authored Markdown '+name)
            if len(re.findall(r'^```',text,re.M))%2:errors.append('unbalanced Markdown fence '+name)
            old=git('show',BASELINE+':'+name) if name in tree else ''
            for problem in local_link_errors(p,text):
                target=problem.split(' -> ',1)[1]
                if ']('+target+')' in old and not any(x in target for x in ['/specs/','planning/','demo-001','merchant-address']):oldlinks.add(problem)
                else:errors.append(problem)
    for name in tree:
        if name.startswith(PROTECTED) and name.endswith('.md') and (ROOT/name).exists():
            for problem in local_link_errors(ROOT/name,body(name)):
                if any(r['path'].split('/')[1] in problem for r in retirement):errors.append('protected dangling retired link '+problem)
    summary=dict(baseline=BASELINE,capabilities=len(rows),roles=len({r['role_id'] for r in rows}),modules=len({r['module_id'] for r in rows}),core=366,defer=sorted(DEFER),logical_specs=len(cards),canonical_triples=len(cards),legacy_records=len(legacy),retirement_overlay_ids=len(overlay),retired_legacy_files=len(retirement),promoted_planning_files=len(promotion),group_ab_bc=len(coverage),group_ab_core=len(accept['core_bc_ids']),portal_supplemental=len(supp),portal_journeys=len(journeys),checked_changed_markdown=checked,protected_source_changes=[n for n in sorted(changed) if n.startswith(PROTECTED)],preexisting_broken_links=sorted(oldlinks),errors=sorted(set(errors)))
    summary['semantic_ownership_status']='CONSISTENT_WITH_EXPLICIT_UNRESOLVED' if not errors and ownership_unresolved else 'ERROR' if errors else 'CONSISTENT'
    summary['ownership_unresolved']=sorted(ownership_unresolved)
    return summary['errors'],summary

if __name__=='__main__':
    sys.stdout.reconfigure(encoding='utf-8');errors,summary=run();print(json.dumps(summary,ensure_ascii=False,indent=2));raise SystemExit(bool(errors))
