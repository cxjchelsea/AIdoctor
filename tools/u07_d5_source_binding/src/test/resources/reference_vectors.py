"""Independent stdlib reference fixture; intentionally does not import Java/SUT output."""
import hashlib
import struct
import unicodedata
from pathlib import Path


def frame(prefix, pairs):
    def item(value):
        if value is None:
            return struct.pack(">i", -1)
        value = unicodedata.normalize("NFC", value).encode("utf-8")
        return struct.pack(">i", len(value)) + value

    return item(prefix) + b"".join(item(name) + item(value) for name, value in pairs)


def sha(value):
    return hashlib.sha256(value).hexdigest()


names = [
    "contract_version", "event_type", "trusted_environment_id", "trusted_profile_id",
    "trusted_tenant_scope_id", "trusted_consultation_id", "trusted_actor_scope_id", "question_id",
    "pending_question_ref", "parent_wait_effect_id", "resume_eligibility_id", "thread_id", "run_id",
    "checkpoint_id", "expected_clinical_state_version", "answer_payload_ref", "answer_payload_digest",
    "target_answer_event_id", "scope_authorization_ref", "actor_binding_ref", "payload_digest",
]
f = ["u07.inbound.v1", "USER_ANSWER", "synthetic-source-binding-ci", "PROFILE-B-SYNTHETIC-STRUCTURAL-NONPROD",
     "synthetic-tenant", "synthetic-consult", "synthetic-actor", "synthetic-question", "synthetic-pending",
     "synthetic-wait", "synthetic-eligibility", "synthetic-thread", "synthetic-run", None, "0", None,
     sha("synthetic-answer:e\u0301".encode()), None, "synthetic-scope", "synthetic-actor-binding", None]
key = "u07-" + sha(frame("u07-storage-key-v1", list(zip(
    names[2:7] + [names[1], "ingress_supplied_stable_idempotency_token"],
    f[2:7] + [f[1], "synthetic-token"]))))
f[15] = "u07db:v1:" + key + ":answer"
f[20] = sha(frame("u07-payload-v1", [("event_type", f[1]), ("answer_payload_ref", f[15]),
                                      ("answer_payload_digest", f[16]), ("payload_schema_version", "u07-payload-v1")]))
content = "\n".join(f"f{i}={v if v is not None else 'NULL'}" for i, v in enumerate(f))
content += f"\nkey={key}\npayload={f[20]}\nbinding={sha(frame('u07-event-binding-v1', list(zip(names, f))))}\n"
path = Path(__file__).with_name("golden.properties")
if "--check" in __import__("sys").argv:
    assert path.read_text() == content, "golden fixture changed"
else:
    path.write_text(content)
