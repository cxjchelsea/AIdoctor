import importlib.util
import pathlib
import unittest

module_path = pathlib.Path(__file__).with_name("runtime_readiness_http.py")
spec = importlib.util.spec_from_file_location("runtime_readiness_http", module_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RuntimeReadinessHttpContractTest(unittest.TestCase):
    def document(self, missing=()):
        values = {key: key not in missing for key in module.REQUIRED}
        ready = all(values.values())
        return {
            "components": values,
            "status": "COMPONENTS_PRESENT_NOT_E2E_VERIFIED"
            if ready else "MISSING_REQUIRED_COMPONENTS",
            "patientDeliveryAllowed": False,
            "fullClinicalE2eVerified": False,
        }

    def test_all_components_still_not_clinical_e2e(self):
        result = module.assess(self.document())
        self.assertTrue(result["hostComponentsPresent"])
        self.assertFalse(result["fullClinicalE2eVerified"])

    def test_missing_owner_reports_precise_failure(self):
        result = module.assess(self.document({"independentClinicalHost"}))
        self.assertFalse(result["hostComponentsPresent"])
        self.assertEqual(["independentClinicalHost"], result["missingComponents"])

    def test_missing_schema_is_rejected(self):
        data = self.document()
        del data["components"]["verifiedGateCScopeAuthority"]
        with self.assertRaises(ValueError):
            module.assess(data)

    def test_bogus_patient_delivery_and_full_e2e_are_rejected(self):
        for key in ("patientDeliveryAllowed", "fullClinicalE2eVerified"):
            data = self.document()
            data[key] = True
            with self.assertRaises(ValueError):
                module.assess(data)


if __name__ == "__main__":
    unittest.main()
