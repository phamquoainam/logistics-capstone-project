"""Negative controls for documentation completeness and false-evidence gates."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

PATH = Path(__file__).with_name("validate.py")
spec = importlib.util.spec_from_file_location("v3_validate", PATH)
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)


class MappingIntegrityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.rows = v.read_csv("capability-crosswalk.csv")
        cls.cards = json.loads((v.OUT / "spec-register.json").read_text(encoding="utf-8"))
        cls.source = v.source_rows()

    def check_mutation(self, mutation, expected):
        rows, cards = copy.deepcopy(self.rows), copy.deepcopy(self.cards)
        mutation(rows, cards)
        errors = v.mapping_errors(rows, cards, self.source)
        self.assertTrue(any(expected in x for x in errors), errors)

    def test_valid_vietnamese_encoding(self):
        self.assertFalse(v.encoding_errors("Tiếng Việt: nghiệp vụ và quyền sở hữu"))

    def test_corrupt_vietnamese_encoding(self):
        self.assertTrue(v.encoding_errors("nghi?p v? c?a ngu?n"))

    def test_current_mapping(self):
        self.assertEqual([], v.mapping_errors(self.rows, self.cards, self.source))

    def test_missing_capability(self):
        self.check_mutation(lambda r, c: r.pop(), "BC count")

    def test_duplicate_replacing_missing(self):
        self.check_mutation(lambda r, c: r.__setitem__(-1, copy.deepcopy(r[0])), "BC count")

    def test_missing_role(self):
        self.check_mutation(lambda r, c: [x.update(role_id="R01") for x in r if x["role_id"] == "R33"], "role coverage")

    def test_missing_module(self):
        self.check_mutation(lambda r, c: [x.update(module_id="M01") for x in r if x["module_id"] == "M18"], "module coverage")

    def test_wrong_defer_classification(self):
        self.check_mutation(lambda r, c: r[5].update(classification="CORE"), "exact DEFER")

    def test_primary_owner_not_single(self):
        self.check_mutation(lambda r, c: r[0].update(primary_spec="L003;L004"), "primary owner")

    def test_unknown_domain(self):
        self.check_mutation(lambda r, c: r[0].update(primary_domain_owner="support_microservice"), "primary owner")

    def test_dropped_source_guard(self):
        self.check_mutation(lambda r, c: r[0].update(required_permission_guard="allow everyone"), "dropped guard")

    def test_denial_cannot_be_positive_endpoint(self):
        self.check_mutation(lambda r, c: r[5].update(acceptance_kind="POSITIVE"), "missing denial")

    def test_no_auto_approved_mapping(self):
        self.check_mutation(lambda r, c: r[0].update(mapping_status="APPROVED"), "unauthorized mapping")

    def test_no_false_verified(self):
        self.check_mutation(lambda r, c: r[0].update(implementation_status="VERIFIED"), "unproved target")

    def test_missing_ui_delivery_scope(self):
        self.check_mutation(lambda r, c: c[2].pop("frontend_scope"), "missing card fields")

    def test_hard_dependency_cycle(self):
        self.check_mutation(lambda r, c: c[0].update(hard_dependencies=["L030"]), "hard dependency cycle")

    def test_unknown_dependency(self):
        self.check_mutation(lambda r, c: c[0].update(hard_dependencies=["L999"]), "missing dependency")

    def test_physical_id_reuse(self):
        self.check_mutation(lambda r, c: c[2].update(folder="specs/003-merchant-address"), "canonical path")

    def test_omitted_requirement_id(self):
        self.check_mutation(lambda r, c: r[0].update(target_requirement=""), "requirement ID")

    def test_guest_cannot_own_merchant_application(self):
        self.check_mutation(lambda r, c: next(x for x in r if x['capability_id']=='BC-133').update(role_id='R01'), 'account-first Merchant application')

    def test_station_staff_cannot_be_merchant_owned(self):
        self.check_mutation(lambda r, c: next(x for x in r if x['capability_id']=='BC-230').update(primary_domain_owner='merchant'), 'corrected ownership')

    def test_pickup_has_one_canonical_command_owner(self):
        self.check_mutation(lambda r, c: next(x for x in r if x['capability_id']=='BC-168').update(primary_spec='L003'), 'corrected ownership')

    def test_old_account_type_choice_cannot_survive_in_register(self):
        self.check_mutation(lambda r, c: c[2].update(acceptance='Guest chọn loại→xác minh→đúng profile/org'), 'stale account-type workflow')


class PortalIntegrityTests(unittest.TestCase):
    def setUp(self):
        self.rows = v.read_csv('capability-crosswalk.csv')
        self.coverage = v.csv_at('docs/portal/group-ab-coverage.csv')
        self.supplement = v.csv_at('docs/portal/supplemental-requirements.csv')
        self.journeys = v.csv_at('docs/portal/journeys.csv')
        self.register = json.loads((v.ROOT/'docs/portal/acceptance-register.json').read_text(encoding='utf-8'))

    def errors(self):
        return v.portal_errors(self.coverage,self.supplement,self.journeys,self.rows,self.register)

    def test_current_portal(self):
        self.assertEqual([],self.errors())

    def test_missing_finance_capability(self):
        self.coverage = [r for r in self.coverage if r['bc_id']!='BC-190']
        self.assertIn('portal BC count/coverage',self.errors())

    def test_cannot_omit_anonymous_shipment_denial(self):
        self.register['required_denials'].remove('BC-006')
        self.assertIn('portal acceptance incomplete',self.errors())

    def test_empty_journey_cannot_count_as_coverage(self):
        for j in self.journeys:j['bc_ids']=''
        self.assertTrue(any('journey orphan' in x for x in self.errors()))

    def test_planned_feature_cannot_claim_implemented(self):
        self.coverage[0]['status']='IMPLEMENTED'
        self.assertTrue(any('false portal evidence' in x for x in self.errors()))

    def test_no_fabricated_product_acceptance(self):
        self.register['status']='PASS'
        self.assertIn('fabricated product acceptance',self.errors())

    def test_missing_owner_added_requirement(self):
        self.supplement.pop()
        self.assertIn('supplemental coverage',self.errors())

    def test_single_merchant_spec_cannot_replace_shipping_owner(self):
        next(r for r in self.coverage if r['bc_id']=='BC-139')['canonical_spec']='L003'
        self.assertTrue(any('portal owner mismatch' in x for x in self.errors()))


class ResourceAuthorityTests(unittest.TestCase):
    def setUp(self):
        self.policy = json.loads(v.body('docs/reconciliation/v3/resource-authority-policy.json'))
        index = {r['resource_id']: r for r in self.policy['resources']}
        self.declarations = [dict(scope=scope, orchestrator='spec003', resources=[
            {k:index[rid][k] for k in ['resource_id','authoritative_owner','resolution','decision']}
            for rid in ids]) for scope, ids in self.policy['document_scopes'].items()]

    def errors(self):
        return v.ownership_errors(self.policy, self.declarations)[0]

    def test_identity_account_and_session_authority_accepted(self):
        self.assertEqual([], self.errors())

    def test_bc134_merchant_security_owner_rejected(self):
        for resource in self.declarations[0]['resources']:
            with self.subTest(resource=resource['resource_id']):
                resource['authoritative_owner'] = 'merchant'
                self.assertTrue(self.errors())
                resource['authoritative_owner'] = 'identity'

    def test_merchant_application_org_membership_authority_accepted(self):
        merchant = [r for r in self.policy['resources'] if r['family']=='merchant_business']
        self.assertEqual(5, len(merchant))
        self.assertTrue(all(r['authoritative_owner']=='merchant' for r in merchant))
        self.assertEqual([], self.errors())

    def test_merchant_business_cannot_move_into_identity(self):
        for resource in self.policy['resources']:
            if resource['family']=='merchant_business':
                with self.subTest(resource=resource['resource_id']):
                    resource['authoritative_owner']='identity'
                    self.assertTrue(self.errors())
                    resource['authoritative_owner']='merchant'

    def test_retail_profile_owner_refinement_is_explicit(self):
        errors, unresolved = v.ownership_errors(self.policy,self.declarations)
        self.assertEqual([], errors)
        self.assertEqual([],unresolved)
        resource=next(r for r in self.policy['resources'] if r['family']=='retail_business')
        self.assertEqual('customer_profile',resource['authoritative_owner'])
        self.assertFalse(resource['requires_organization'])
        self.assertFalse(resource['requires_membership'])

    def test_retail_profile_cannot_be_silently_assigned(self):
        resource = next(r for r in self.policy['resources'] if r['family']=='retail_business')
        for owner in ['identity','merchant',None]:
            with self.subTest(owner=owner):
                resource.update(authoritative_owner=owner,resolution='DECLARED',decision=None)
                self.assertTrue(self.errors())

    def test_missing_pending_decision_rejected(self):
        self.declarations[1]['resources'][0]['decision']=None
        self.assertTrue(self.errors())

    def test_undeclared_resource_rejected(self):
        self.declarations[0]['resources'][0]['resource_id']='customer.account_master'
        self.assertTrue(self.errors())

    def test_contradictory_duplicate_authority_rejected(self):
        duplicate=copy.deepcopy(self.declarations[0]['resources'][0])
        duplicate['authoritative_owner']='merchant'
        self.declarations[0]['resources'].append(duplicate)
        self.assertTrue(self.errors())

    def test_unknown_owner_rejected(self):
        self.declarations[0]['resources'][0]['authoritative_owner']='customer'
        self.assertTrue(self.errors())

    def test_shared_orchestrator_grants_no_data_authority(self):
        for owner in ['spec003',['identity','merchant']]:
            with self.subTest(owner=owner):
                self.declarations[0]['resources'][0]['authoritative_owner']=owner
                self.assertTrue(self.errors())

    def test_orchestrator_cannot_grant_itself_authority(self):
        self.policy['orchestrators']['spec003']['grants_data_authority']=True
        self.assertTrue(self.errors())

    def test_station_hub_dispatch_accepted_merchant_rejected(self):
        self.assertEqual([], self.errors())
        for resource in self.policy['resources']:
            if resource['family']=='carrier_network':
                with self.subTest(resource=resource['resource_id']):
                    resource['authoritative_owner']='merchant'
                    self.assertTrue(self.errors())
                    resource['authoritative_owner']='dispatch'

    def test_missing_resource_scope_or_capability_policy_rejected(self):
        for key in ['resources','document_scopes','capability_authorities']:
            with self.subTest(key=key):
                policy=copy.deepcopy(self.policy)
                policy.pop(key)
                self.assertTrue(v.ownership_errors(policy,self.declarations)[0])

    def test_current_design_and_tasks_declarations_accepted(self):
        for path in self.policy['declaration_documents']:
            with self.subTest(path=path):
                errors, unresolved=v.ownership_document_errors(self.policy,v.body(path))
                self.assertEqual([],errors)
                self.assertEqual([],unresolved)

    def test_personal_profile_cannot_require_merchant_tenant(self):
        resource=next(r for r in self.policy['resources'] if r['family']=='retail_business')
        for field in ['requires_organization','requires_membership']:
            with self.subTest(field=field):
                resource[field]=True
                self.assertTrue(self.errors())
                resource[field]=False

    def test_original_bc134_design_and_db_task_regressions_rejected(self):
        import re
        for path in self.policy['declaration_documents']:
            with self.subTest(path=path):
                original=v.git('show','8e8327273fdcdffb3667ee0c6d15fa43f9a0eb89:'+path)
                pattern=r'^#{2,3} (?:DES-|REQ-V3-)BC-134\b.*?(?=^#{1,3} |\Z)'
                old_section=re.search(pattern,original,re.M|re.S).group()
                regressed=re.sub(pattern,lambda _:old_section,v.body(path),flags=re.M|re.S)
                self.assertTrue(v.ownership_document_errors(self.policy,regressed)[0])

    def test_wording_does_not_override_typed_authority(self):
        declaration=copy.deepcopy(self.declarations[0])
        declaration['resources'][0]['authoritative_owner']='merchant'
        text='### DES-BC-134\nWorkflow calls approved public interfaces.\n```ownership-json\n'+json.dumps(declaration)+'\n```\n'
        text+='### DES-BC-135\n```ownership-json\n'+json.dumps(self.declarations[1])+'\n```\n'
        self.assertTrue(v.ownership_document_errors(self.policy,text)[0])

    def test_malformed_or_duplicate_document_binding_rejected(self):
        text=v.body(self.policy['declaration_documents'][0])
        self.assertTrue(v.ownership_document_errors(self.policy,text+'\n```ownership-json\n{invalid}\n```\n')[0])
        self.assertTrue(v.ownership_document_errors(self.policy,text+'\n```ownership-json\n'+json.dumps(self.declarations[0])+'\n```\n')[0])

    def test_binding_must_belong_to_its_requirement_section(self):
        text='### DES-BC-134\n```ownership-json\n'+json.dumps(self.declarations[1])+'\n```\n'
        text+='### DES-BC-135\n```ownership-json\n'+json.dumps(self.declarations[0])+'\n```\n'
        self.assertTrue(v.ownership_document_errors(self.policy,text)[0])


if __name__ == "__main__":
    unittest.main()
