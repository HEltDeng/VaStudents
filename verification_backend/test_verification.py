from dataclasses import replace
from datetime import datetime, timedelta, timezone
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from verification import (Actor, Config, Conclusions, Conflict, DataPolicy, Denied,
                          DevelopmentAuth, Environment, Finding, InvalidTransition,
                          LocalMaterialStore, Role, Status, VerificationService)


PASS = Conclusions(Finding.PASS, Finding.PASS, Finding.PASS)


class VerificationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.now = datetime(2026, 10, 3, tzinfo=timezone.utc)
        self.auth = DevelopmentAuth(Environment.DEVELOPMENT)
        self.student = self.auth.login()
        self.other = self.auth.login()
        self.reviewer = self.auth.login(Role.REVIEWER)
        self.store = LocalMaterialStore(Path(self.tmp.name) / "private")
        self.service = VerificationService(Config(), self.auth, self.store, lambda: self.now)

    def submit(self):
        return self.service.submit_test_fixture(self.student, "synthetic-student", "submission_1")

    def approve(self, record):
        return self.service.review(self.reviewer, record.id, Status.APPROVED, PASS, "review_0001")

    def test_other_owner_cannot_read_metadata_or_material(self):
        record = self.submit()
        for method in (self.service.get, self.service.read_material):
            with self.assertRaises(Denied):
                method(self.other, record.id)
        with self.assertRaises(Denied):
            self.store.read(self.auth.resolve(self.other), record.material, self.now)
        self.assertIn(b"SYNTHETIC TEST", self.service.read_material(self.student, record.id))

    def test_unknown_id_and_unauthorized_id_have_same_error(self):
        record = self.submit()
        messages = []
        for record_id in (record.id, "unknown"):
            with self.assertRaises(Denied) as error:
                self.service.get(self.other, record_id)
            messages.append(str(error.exception))
        self.assertEqual(messages[0], messages[1])

    def test_student_cannot_review_or_revoke(self):
        record = self.submit()
        with self.assertRaises(Denied):
            self.service.review(self.student, record.id, Status.APPROVED, PASS, "review_0001")
        with self.assertRaises(Denied):
            self.service.revoke(self.student, record.id, "revoke_001")
        with self.assertRaises(Denied):
            self.service.read_material(self.reviewer, record.id)

    def test_self_review_denied_even_if_auth_provider_escalates_role(self):
        record = self.submit()
        self.auth._sessions[self.reviewer] = Actor(record.owner, Role.REVIEWER)
        with self.assertRaises(Denied):
            self.approve(record)

    def test_three_conclusions_are_independent(self):
        record = self.submit()
        findings = Conclusions(Finding.PASS, Finding.FAIL, Finding.UNKNOWN)
        with self.assertRaises(InvalidTransition):
            self.service.review(self.reviewer, record.id, Status.APPROVED, findings, "review_0001")
        rejected = self.service.review(self.reviewer, record.id, Status.REJECTED, findings, "review_0002")
        self.assertEqual(rejected.conclusions, findings)
        self.assertEqual(rejected.status, Status.REJECTED)
        with self.assertRaises(InvalidTransition):
            self.approve(record)

    def test_approved_cannot_be_reviewed_twice(self):
        record = self.submit()
        self.approve(record)
        with self.assertRaises(InvalidTransition):
            self.service.review(self.reviewer, record.id, Status.REJECTED, PASS, "review_0002")

    def test_pending_and_approved_expire_at_exact_boundary(self):
        record = self.submit()
        self.now += timedelta(days=7)
        self.assertEqual(self.service.get(self.student, record.id).status, Status.EXPIRED)
        with self.assertRaises(InvalidTransition):
            self.approve(record)
        next_record = self.service.submit_test_fixture(self.other, "synthetic-student", "submission_2")
        self.approve(next_record)
        self.now += timedelta(days=7)
        self.assertEqual(self.service.get(self.other, next_record.id).status, Status.EXPIRED)

    def test_revoke_is_terminal_and_immediately_deletes_material(self):
        record = self.submit()
        self.approve(record)
        revoked = self.service.revoke(self.reviewer, record.id, "revoke_001")
        self.assertEqual(revoked.status, Status.REVOKED)
        self.assertTrue(revoked.material_deleted)
        self.assertEqual(list(self.store.root.iterdir()), [])
        with self.assertRaises(Denied):
            self.service.read_material(self.student, record.id)
        self.now += timedelta(days=100)
        self.assertEqual(self.service.get(self.student, record.id).status, Status.REVOKED)
        with self.assertRaises(InvalidTransition):
            self.service.revoke(self.reviewer, record.id, "revoke_002")

    def test_retention_deletes_without_overwriting_independent_approval(self):
        self.service = VerificationService(Config(retention=timedelta(hours=1)),
                                           self.auth, self.store, lambda: self.now)
        record = self.submit()
        self.approve(record)
        self.now += timedelta(hours=1)
        self.assertEqual(self.service.purge_expired_materials(), 1)
        self.assertEqual(self.service.purge_expired_materials(), 0)
        self.assertEqual(list(self.store.root.iterdir()), [])
        refreshed = self.service.get(self.student, record.id)
        self.assertTrue(refreshed.material_deleted)
        self.assertEqual(refreshed.status, Status.APPROVED)

    def test_deleted_pending_material_cannot_be_approved(self):
        self.service = VerificationService(Config(retention=timedelta(hours=1)),
                                           self.auth, self.store, lambda: self.now)
        record = self.submit()
        self.now += timedelta(hours=1)
        with self.assertRaises(InvalidTransition):
            self.approve(record)

    def test_submit_review_and_revoke_retries_are_idempotent(self):
        record = self.submit()
        self.assertEqual(self.submit(), record)
        self.assertEqual(len(list(self.store.root.iterdir())), 1)
        approved = self.approve(record)
        self.assertEqual(self.approve(record), approved)
        revoked = self.service.revoke(self.reviewer, record.id, "revoke_001")
        self.assertEqual(self.service.revoke(self.reviewer, record.id, "revoke_001"), revoked)
        # A stale retry must never resurrect an earlier successful approval.
        self.assertEqual(self.approve(record).status, Status.REVOKED)
        self.assertEqual(len(self.service.audit), 3)

    def test_reused_key_with_different_payload_is_rejected(self):
        record = self.submit()
        with self.assertRaises(Conflict):
            self.service.submit_test_fixture(self.student, "synthetic-incomplete", "submission_1")
        self.approve(record)
        with self.assertRaises(Conflict):
            self.service.review(self.reviewer, record.id, Status.REJECTED, PASS, "review_0001")

    def test_same_key_for_different_owners_is_independent(self):
        first = self.submit()
        second = self.service.submit_test_fixture(self.other, "synthetic-student", "submission_1")
        self.assertNotEqual(first.id, second.id)

    def test_no_arbitrary_bytes_files_or_fixture_names(self):
        for name in ("../document.pdf", "real-student", b"VASTUDENTS SYNTHETIC TEST MATERIAL ONLY"):
            with self.assertRaises(Denied):
                self.service.submit_test_fixture(self.student, name, "invalid_fixture")
        self.assertEqual(list(self.store.root.iterdir()), [])

    def test_path_traversal_and_integrity_tampering_rejected(self):
        record = self.submit()
        actor = self.auth.resolve(self.student)
        with self.assertRaises(Denied):
            self.store.read(actor, replace(record.material, id="../anything"), self.now)
        self.store._path(record.material.id).write_bytes(b"unexpected mutation")
        with self.assertRaises(Denied):
            self.service.read_material(self.student, record.id)

    def test_production_disables_dev_login_skip_intake_and_review(self):
        with self.assertRaises(Denied):
            DevelopmentAuth(Environment.PRODUCTION).login()
        service = VerificationService(Config(environment=Environment.PRODUCTION),
                                      self.auth, self.store, lambda: self.now)
        operations = (
            lambda: service.development_skip(self.student),
            lambda: service.submit_test_fixture(self.student, "synthetic-student", "submission_1"),
            lambda: service.review(self.reviewer, "unknown", Status.APPROVED, PASS, "review_0001"),
            lambda: service.revoke(self.reviewer, "unknown", "revoke_001"),
            lambda: service.get(self.student, "unknown"),
        )
        for operation in operations:
            with self.assertRaises(Denied):
                operation()

    def test_real_intake_always_closed_even_with_client_flags(self):
        with self.assertRaises(Denied):
            self.service.submit_real_material(self.student, b"test", production=False,
                                               allow_real=True, policy_confirmed=True)

        class FutureRealAuth:
            synthetic = False

            def resolve(self, session):
                return Actor("test-adapter-only", Role.STUDENT)

        for policy in (None, DataPolicy("scope", "reviewer", 7, False),
                       DataPolicy("scope", "reviewer", 7, True)):
            service = VerificationService(Config(environment=Environment.PRODUCTION, data_policy=policy),
                                          FutureRealAuth(), self.store, lambda: self.now)
            with self.assertRaises(Denied):
                service.submit_real_material("test-session", b"not a document")
        self.assertEqual(list(self.store.root.iterdir()), [])

    def test_skip_does_not_grant_identity_or_redemption(self):
        result = self.service.development_skip(self.student)
        self.assertTrue(result["browse_public_offers"])
        self.assertFalse(result["student_verified"])
        self.assertFalse(result["merchant_redemption_enabled"])

    def test_logs_contain_no_session_owner_or_material_content(self):
        record = self.submit()
        self.approve(record)
        logged = repr(self.service.audit)
        for secret in (self.student, self.reviewer, record.owner, record.material.id,
                       "FICTIONAL STUDENT"):
            self.assertNotIn(secret, logged)
        self.assertEqual(set(self.service.audit[0]),
                         {"event", "verification_id", "status", "synthetic"})

    def test_invalid_auth_config_findings_and_lifetimes_rejected(self):
        with self.assertRaises(Denied):
            self.service.development_skip("invented-session")
        with self.assertRaises(ValueError):
            Config(environment="development")
        for duration in (timedelta(0), timedelta(days=-1), timedelta(days=31)):
            with self.assertRaises(ValueError):
                Config(retention=duration)
        record = self.submit()
        with self.assertRaises(ValueError):
            self.service.review(self.reviewer, record.id, Status.APPROVED,
                                Conclusions("pass", "pass", "pass"), "review_0001")


if __name__ == "__main__":
    unittest.main(verbosity=2)
