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
        self.check_mutation(lambda r, c: c[2].update(folder="specs/003-merchant-address"), "legacy numeric ID reuse")

    def test_omitted_requirement_id(self):
        self.check_mutation(lambda r, c: r[0].update(target_requirement=""), "requirement ID")


if __name__ == "__main__":
    unittest.main()
