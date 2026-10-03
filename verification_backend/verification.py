"""VaStudents synthetic-only verification domain. No network or real-document intake."""
from __future__ import annotations

from dataclasses import dataclass, replace
from datetime import datetime, timedelta, timezone
from enum import Enum
from hashlib import sha256
from pathlib import Path
from typing import Callable, Protocol
import os
import re
import secrets


class Denied(Exception):
    pass


class InvalidTransition(Exception):
    pass


class Conflict(Exception):
    pass


class Environment(str, Enum):
    DEVELOPMENT = "development"
    PRODUCTION = "production"


class Role(str, Enum):
    STUDENT = "student"
    REVIEWER = "reviewer"


class Status(str, Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"
    EXPIRED = "expired"
    REVOKED = "revoked"


class Finding(str, Enum):
    UNKNOWN = "unknown"
    PASS = "pass"
    FAIL = "fail"


@dataclass(frozen=True)
class Conclusions:
    identity: Finding = Finding.UNKNOWN
    enrollment: Finding = Finding.UNKNOWN
    authenticity: Finding = Finding.UNKNOWN

    def all_pass(self) -> bool:
        return all(x is Finding.PASS for x in
                   (self.identity, self.enrollment, self.authenticity))


@dataclass(frozen=True)
class Actor:
    subject: str
    role: Role


class AuthProvider(Protocol):
    synthetic: bool

    def resolve(self, session: str) -> Actor: ...


class DevelopmentAuth:
    """Local harness auth; not an internet-facing authentication implementation."""
    synthetic = True

    def __init__(self, environment: Environment):
        self.environment = environment
        self._sessions: dict[str, Actor] = {}

    def login(self, role: Role = Role.STUDENT) -> str:
        if self.environment is not Environment.DEVELOPMENT:
            raise Denied("development_auth_disabled")
        if not isinstance(role, Role):
            raise Denied("invalid_role")
        session = secrets.token_urlsafe(32)
        self._sessions[session] = Actor(secrets.token_hex(16), role)
        return session

    def resolve(self, session: str) -> Actor:
        if self.environment is not Environment.DEVELOPMENT:
            raise Denied("development_auth_disabled")
        try:
            return self._sessions[session]
        except (KeyError, TypeError):
            raise Denied("authentication_required") from None


@dataclass(frozen=True)
class DataPolicy:
    """Server-side policy, never supplied by a mobile client."""
    data_scope: str
    reviewer_organization: str
    retention_days: int
    confirmed: bool = False

    def valid(self) -> bool:
        return (self.confirmed and bool(self.data_scope.strip())
                and bool(self.reviewer_organization.strip())
                and 1 <= self.retention_days <= 30)


@dataclass(frozen=True)
class Config:
    environment: Environment = Environment.DEVELOPMENT
    retention: timedelta = timedelta(days=7)
    pending_lifetime: timedelta = timedelta(days=7)
    max_approval_lifetime: timedelta = timedelta(days=30)
    data_policy: DataPolicy | None = None

    def __post_init__(self):
        if not isinstance(self.environment, Environment):
            raise ValueError("environment must be explicit enum")
        for duration in (self.retention, self.pending_lifetime,
                         self.max_approval_lifetime):
            if duration <= timedelta(0) or duration > timedelta(days=30):
                raise ValueError("bounded positive lifetime required")


# Exact fixtures are authored test bytes, not arbitrary user supplied files.
TEST_FIXTURES = {
    "synthetic-student": b"VASTUDENTS SYNTHETIC TEST MATERIAL ONLY\nFICTIONAL STUDENT\n",
    "synthetic-incomplete": b"VASTUDENTS SYNTHETIC TEST MATERIAL ONLY\nINCOMPLETE FIXTURE\n",
}


@dataclass(frozen=True)
class Material:
    id: str
    owner: str
    digest: str
    delete_at: datetime


class MaterialStore(Protocol):
    def put_fixture(self, owner: Actor, fixture: str, delete_at: datetime) -> Material: ...
    def read(self, actor: Actor, material: Material, now: datetime) -> bytes: ...
    def delete(self, material: Material) -> None: ...


class LocalMaterialStore:
    """Private directory adapter; deployment must provision OS-level access control."""

    def __init__(self, root: Path):
        self.root = root.resolve()
        self.root.mkdir(parents=True, exist_ok=True, mode=0o700)
        # chmod is meaningful on POSIX; Windows requires a deployment ACL (README).
        if os.name != "nt":
            self.root.chmod(0o700)

    def _path(self, material_id: str) -> Path:
        if not re.fullmatch(r"[a-f0-9]{32}", material_id):
            raise Denied("invalid_material_reference")
        path = self.root / (material_id + ".bin")
        if path.is_symlink() or path.resolve().parent != self.root:
            raise Denied("invalid_material_reference")
        return path

    def put_fixture(self, owner: Actor, fixture: str, delete_at: datetime) -> Material:
        if owner.role is not Role.STUDENT or fixture not in TEST_FIXTURES:
            raise Denied("only_synthetic_fixtures_allowed")
        content = TEST_FIXTURES[fixture]
        material = Material(secrets.token_hex(16), owner.subject,
                            sha256(content).hexdigest(), delete_at)
        descriptor = os.open(self._path(material.id), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        with os.fdopen(descriptor, "wb") as handle:
            handle.write(content)
        return material

    def read(self, actor: Actor, material: Material, now: datetime) -> bytes:
        if actor.subject != material.owner or actor.role is not Role.STUDENT:
            raise Denied("material_access_denied")
        if now >= material.delete_at:
            self.delete(material)
            raise Denied("material_expired")
        try:
            content = self._path(material.id).read_bytes()
        except FileNotFoundError:
            raise Denied("material_unavailable") from None
        if sha256(content).hexdigest() != material.digest:
            raise Denied("material_integrity_failure")
        return content

    def delete(self, material: Material) -> None:
        self._path(material.id).unlink(missing_ok=True)


@dataclass(frozen=True)
class Verification:
    id: str
    owner: str
    material: Material
    fixture: str
    status: Status
    created_at: datetime
    expires_at: datetime
    conclusions: Conclusions = Conclusions()
    synthetic: bool = True
    material_deleted: bool = False


class VerificationService:
    """In-memory record repository; all client inputs cross session/role checks.

    No student credentials, merchant redemption, or authoritative validation is issued.
    """

    def __init__(self, config: Config, auth: AuthProvider, store: MaterialStore,
                 clock: Callable[[], datetime] = lambda: datetime.now(timezone.utc)):
        self.config, self.auth, self.store, self.clock = config, auth, store, clock
        self._records: dict[str, Verification] = {}
        self._operations: dict[tuple[str, str], tuple[tuple, str]] = {}
        self.audit: list[dict[str, str]] = []

    def _actor(self, session: str, role: Role | None = None) -> Actor:
        if self.config.environment is Environment.PRODUCTION and self.auth.synthetic:
            raise Denied("production_requires_real_auth_provider")
        actor = self.auth.resolve(session)
        if role is not None and actor.role is not role:
            raise Denied("role_not_allowed")
        return actor

    def _event(self, name: str, record: Verification):
        # No session, subject, original filename, contents, or free text in logs.
        self.audit.append({"event": name, "verification_id": record.id,
                           "status": record.status.value, "synthetic": "true"})

    def _key(self, actor: Actor, key: str, fingerprint: tuple) -> Verification | None:
        if not isinstance(key, str) or not re.fullmatch(r"[A-Za-z0-9_-]{8,64}", key):
            raise ValueError("opaque_idempotency_key_required")
        previous = self._operations.get((actor.subject, key))
        if previous:
            if previous[0] != fingerprint:
                raise Conflict("idempotency_key_reused")
            return self._refresh(self._records[previous[1]])
        return None

    def _remember(self, actor: Actor, key: str, fingerprint: tuple, record: Verification):
        self._operations[(actor.subject, key)] = (fingerprint, record.id)
        self._records[record.id] = record

    def _refresh(self, record: Verification) -> Verification:
        now = self.clock()
        if now >= record.material.delete_at and not record.material_deleted:
            self.store.delete(record.material)
            record = replace(record, material_deleted=True)
            self._event("material_deleted", record)
        if now >= record.expires_at and record.status in (Status.PENDING, Status.APPROVED):
            record = replace(record, status=Status.EXPIRED)
            self._event("verification_expired", record)
        self._records[record.id] = record
        return record

    def _find(self, actor: Actor, verification_id: str, reviewer: bool = False) -> Verification:
        record = self._records.get(verification_id)
        if record is None or (not reviewer and record.owner != actor.subject):
            raise Denied("verification_unavailable")
        return self._refresh(record)

    def submit_test_fixture(self, session: str, fixture: str, idempotency_key: str) -> Verification:
        if self.config.environment is not Environment.DEVELOPMENT:
            raise Denied("synthetic_intake_disabled")
        actor = self._actor(session, Role.STUDENT)
        fingerprint = ("submit", fixture)
        cached = self._key(actor, idempotency_key, fingerprint)
        if cached:
            return cached
        now = self.clock()
        material = self.store.put_fixture(actor, fixture, now + self.config.retention)
        record = Verification(secrets.token_hex(16), actor.subject, material, fixture,
                              Status.PENDING, now, now + self.config.pending_lifetime)
        self._remember(actor, idempotency_key, fingerprint, record)
        self._event("synthetic_submitted", record)
        return record

    def submit_real_material(self, session: str, *args, **kwargs):
        self._actor(session, Role.STUDENT)
        if self.auth.synthetic or self.config.data_policy is None or not self.config.data_policy.valid():
            raise Denied("real_intake_requires_confirmed_policy_and_real_auth")
        # Deliberate hard stop, including when a future real auth adapter is injected.
        raise Denied("real_intake_not_implemented")

    def development_skip(self, session: str):
        if self.config.environment is not Environment.DEVELOPMENT:
            raise Denied("development_skip_disabled")
        self._actor(session, Role.STUDENT)
        return {"browse_public_offers": True, "student_verified": False,
                "merchant_redemption_enabled": False, "synthetic": True}

    def get(self, session: str, verification_id: str) -> Verification:
        return self._find(self._actor(session, Role.STUDENT), verification_id)

    def read_material(self, session: str, verification_id: str) -> bytes:
        actor = self._actor(session, Role.STUDENT)
        record = self._find(actor, verification_id)
        return self.store.read(actor, record.material, self.clock())

    def review(self, session: str, verification_id: str, decision: Status,
               conclusions: Conclusions, idempotency_key: str,
               validity: timedelta = timedelta(days=7)) -> Verification:
        if self.config.environment is not Environment.DEVELOPMENT:
            raise Denied("synthetic_review_disabled")
        actor = self._actor(session, Role.REVIEWER)
        record = self._find(actor, verification_id, reviewer=True)
        if record.owner == actor.subject:
            raise Denied("self_review_disabled")
        if not isinstance(conclusions, Conclusions) or not all(isinstance(x, Finding) for x in
            (conclusions.identity, conclusions.enrollment, conclusions.authenticity)):
            raise ValueError("three_explicit_conclusions_required")
        fingerprint = ("review", verification_id, decision, conclusions, validity)
        cached = self._key(actor, idempotency_key, fingerprint)
        if cached:
            return cached
        if record.status is not Status.PENDING or decision not in (Status.APPROVED, Status.REJECTED):
            raise InvalidTransition("review_requires_pending")
        if record.material_deleted:
            raise InvalidTransition("review_material_unavailable")
        if decision is Status.APPROVED and not conclusions.all_pass():
            raise InvalidTransition("approval_requires_all_three_test_findings")
        if not timedelta(0) < validity <= self.config.max_approval_lifetime:
            raise ValueError("invalid_approval_lifetime")
        record = replace(record, status=decision, conclusions=conclusions,
                         expires_at=self.clock() + validity)
        self._remember(actor, idempotency_key, fingerprint, record)
        self._event("synthetic_reviewed", record)
        return record

    def revoke(self, session: str, verification_id: str, idempotency_key: str) -> Verification:
        if self.config.environment is not Environment.DEVELOPMENT:
            raise Denied("synthetic_review_disabled")
        actor = self._actor(session, Role.REVIEWER)
        record = self._find(actor, verification_id, reviewer=True)
        if record.owner == actor.subject:
            raise Denied("self_review_disabled")
        fingerprint = ("revoke", verification_id)
        cached = self._key(actor, idempotency_key, fingerprint)
        if cached:
            return cached
        if record.status not in (Status.PENDING, Status.APPROVED):
            raise InvalidTransition("record_not_revocable")
        self.store.delete(record.material)
        record = replace(record, status=Status.REVOKED, material_deleted=True)
        self._remember(actor, idempotency_key, fingerprint, record)
        self._event("synthetic_revoked", record)
        return record

    def purge_expired_materials(self) -> int:
        """Trusted maintenance entry point; caller must schedule it in a future server."""
        count = 0
        for record in tuple(self._records.values()):
            refreshed = self._refresh(record)
            count += int(not record.material_deleted and refreshed.material_deleted)
        return count
