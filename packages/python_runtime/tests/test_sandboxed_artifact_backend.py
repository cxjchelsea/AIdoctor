"""POSTFREEZE-03B-I：有界 sandbox filesystem ArtifactPort 安全与行为证明。"""

from __future__ import annotations

import os
from pathlib import Path

import pytest
from aidoctor_shared_contracts import SourceArtifact

from packages.python_runtime.artifacts import (
    ERROR_ARTIFACT_CHECKSUM_MISMATCH,
    ERROR_ARTIFACT_FILE_MISSING,
    ERROR_ARTIFACT_ID_MISMATCH,
    ERROR_ARTIFACT_NOT_REGULAR_FILE,
    ERROR_ARTIFACT_PATH_ESCAPE,
    ERROR_ARTIFACT_PATH_INVALID,
    ERROR_ARTIFACT_PHI_REJECTED,
    ERROR_ARTIFACT_SANDBOX_ROOT_INVALID,
    ERROR_ARTIFACT_SIZE_MISMATCH,
    ERROR_ARTIFACT_STORAGE_REF_UNSUPPORTED,
    ERROR_ARTIFACT_SYMLINK_REJECTED,
    ERROR_ARTIFACT_TOO_LARGE,
    ERROR_ARTIFACT_UNKNOWN,
    ERROR_ARTIFACT_VERSION_MISMATCH,
    MAX_ARTIFACT_BYTES,
    ArtifactResolutionError,
    SandboxedArtifactRecord,
    SandboxedFilesystemArtifactPort,
    sha256_hex,
)

REQUIRE_SYMLINK = os.environ.get("AIDOCTOR_REQUIRE_SANDBOX_SYMLINK_TEST") == "1"
_OPAQUE_REF = "artifact-opaque-sandbox-1"
_RELATIVE_PATH = "objects/slot-001/payload.bin"
_PAYLOAD = b"SANDBOX-PAYLOAD-BYTES"


def _metadata(
    *,
    artifact_id: str = _OPAQUE_REF,
    content: bytes = _PAYLOAD,
    size_bytes: int | None = None,
    checksum_value: str | None = None,
    storage_ref: str = "artifact://engineering-sandbox/artifact-opaque-sandbox-1",
    original_filename: str = "display-only.bin",
    sensitivity: str = "INTERNAL",
) -> SourceArtifact:
    payload = content
    return SourceArtifact.model_validate(
        {
            "contract_version": "1.0.0",
            "artifact_id": artifact_id,
            "artifact_type": "UPLOAD",
            "owner_ref": "synthetic-owner-sandbox-1",
            "content_type": "application/octet-stream",
            "original_filename": original_filename,
            "size_bytes": len(payload) if size_bytes is None else size_bytes,
            "checksum": {
                "algorithm": "SHA-256",
                "value": sha256_hex(payload) if checksum_value is None else checksum_value,
            },
            "storage_ref": storage_ref,
            "created_at": "2026-08-20T08:00:00Z",
            "processing_status": "RECEIVED",
            "derived_artifacts": [],
            "sensitivity": sensitivity,
            "retention_class": "ENGINEERING_SYNTHETIC",
        }
    )


def _port(
    sandbox: Path,
    relative_path: str = _RELATIVE_PATH,
    **metadata_kwargs: object,
) -> SandboxedFilesystemArtifactPort:
    metadata = _metadata(**metadata_kwargs)
    return SandboxedFilesystemArtifactPort(
        sandbox,
        {
            (_OPAQUE_REF, 1): SandboxedArtifactRecord(
                metadata=metadata,
                relative_path=relative_path,
            )
        },
    )


def _write_payload(sandbox: Path, relative_path: str = _RELATIVE_PATH, content: bytes = _PAYLOAD) -> Path:
    target = sandbox.joinpath(*relative_path.split("/"))
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)
    return target


def _require_symlink(tmp_path: Path) -> None:
    probe_target = tmp_path / "symlink-probe-target"
    probe_link = tmp_path / "symlink-probe-link"
    probe_target.write_bytes(b"probe")
    try:
        probe_link.symlink_to(probe_target)
    except OSError as exc:
        if REQUIRE_SYMLINK:
            pytest.fail(f"AIDOCTOR_REQUIRE_SANDBOX_SYMLINK_TEST=1 but symlink failed: {exc}")
        pytest.skip("local environment cannot create symlinks")


def test_sandboxed_backend_reads_real_file_with_opaque_ref(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    (sandbox / "unregistered.bin").write_bytes(b"secret-unregistered")

    resolved = _port(sandbox).resolve(_OPAQUE_REF, 1)

    assert resolved.content == _PAYLOAD
    assert resolved.metadata.artifact_id == _OPAQUE_REF
    assert not (sandbox / _OPAQUE_REF).exists()
    assert not (sandbox / resolved.metadata.original_filename).exists()
    print("SANDBOXED_ARTIFACT_BACKEND_REAL_FILE_READ=YES", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_REF_ID_OPAQUE=YES", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_REF_ID_USED_AS_PATH=NO", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_STORAGE_REF_USED_AS_PATH=NO", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_ORIGINAL_FILENAME_USED_AS_PATH=NO", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_CHECKSUM_VALIDATED=YES", flush=True)
    print("SANDBOXED_ARTIFACT_BACKEND_SIZE_VALIDATED=YES", flush=True)


def test_unknown_ref_does_not_reach_unregistered_sandbox_file(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    (sandbox / "objects" / "slot-001" / "other.bin").write_bytes(b"other")

    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve("artifact-unknown-ref", 1)
    assert caught.value.error_code == ERROR_ARTIFACT_UNKNOWN
    print("SANDBOXED_ARTIFACT_BACKEND_UNREGISTERED_FILE_UNREACHABLE=YES", flush=True)


def test_version_mismatch_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 99)
    assert caught.value.error_code == ERROR_ARTIFACT_VERSION_MISMATCH


def test_absolute_posix_path_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, relative_path="/tmp/escape.bin").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_PATH_INVALID
    print("SANDBOXED_ARTIFACT_BACKEND_ABSOLUTE_PATH_REJECTED=YES", flush=True)


def test_windows_drive_path_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, relative_path="C:/windows/system32/escape.bin").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_PATH_INVALID


def test_traversal_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    outside = tmp_path / "outside.bin"
    outside.write_bytes(b"outside")
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, relative_path="../outside.bin").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code in {ERROR_ARTIFACT_PATH_INVALID, ERROR_ARTIFACT_PATH_ESCAPE}
    print("SANDBOXED_ARTIFACT_BACKEND_PATH_TRAVERSAL_REJECTED=YES", flush=True)


def test_nested_traversal_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, relative_path="objects/slot-001/../../../outside.bin").resolve(
            _OPAQUE_REF, 1
        )
    assert caught.value.error_code in {ERROR_ARTIFACT_PATH_INVALID, ERROR_ARTIFACT_PATH_ESCAPE}


def test_sandbox_root_escape_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, relative_path="objects/../../outside.bin").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code in {ERROR_ARTIFACT_PATH_INVALID, ERROR_ARTIFACT_PATH_ESCAPE}
    print("SANDBOXED_ARTIFACT_BACKEND_ROOT_ESCAPE_REJECTED=YES", flush=True)


def test_symlink_file_escape_rejected(tmp_path: Path) -> None:
    _require_symlink(tmp_path)
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    outside = tmp_path / "outside-secret.bin"
    outside.write_bytes(b"escaped-secret")
    link = sandbox / "objects" / "slot-001" / "payload.bin"
    link.parent.mkdir(parents=True)
    link.symlink_to(outside)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_SYMLINK_REJECTED
    print("SANDBOXED_ARTIFACT_BACKEND_SYMLINK_ESCAPE_REJECTED=YES", flush=True)


def test_symlink_directory_escape_rejected(tmp_path: Path) -> None:
    _require_symlink(tmp_path)
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    outside_dir = tmp_path / "outside-dir"
    outside_dir.mkdir()
    (outside_dir / "payload.bin").write_bytes(_PAYLOAD)
    objects = sandbox / "objects"
    objects.symlink_to(outside_dir, target_is_directory=True)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_SYMLINK_REJECTED


def test_missing_file_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    (sandbox / "objects" / "slot-001").mkdir(parents=True)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_FILE_MISSING


def test_directory_rejected_as_payload(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    directory = sandbox / "objects" / "slot-001" / "payload.bin"
    directory.mkdir(parents=True)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_NOT_REGULAR_FILE


def test_unsupported_storage_ref_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, storage_ref="s3://bucket/object").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_STORAGE_REF_UNSUPPORTED
    print("SANDBOXED_ARTIFACT_BACKEND_UNSUPPORTED_STORAGE_REF_REJECTED=YES", flush=True)


def test_https_storage_ref_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, storage_ref="https://example.invalid/object").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_STORAGE_REF_UNSUPPORTED


def test_oversized_file_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    oversized = b"A" * (MAX_ARTIFACT_BYTES + 1)
    _write_payload(sandbox, content=oversized)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, content=oversized).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_TOO_LARGE
    print("SANDBOXED_ARTIFACT_BACKEND_OVERSIZED_REJECTED=YES", flush=True)


def test_size_mismatch_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, size_bytes=1).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_SIZE_MISMATCH


def test_checksum_mismatch_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, checksum_value="0" * 64).resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_CHECKSUM_MISMATCH


def test_phi_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, sensitivity="PHI").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_PHI_REJECTED
    print("SANDBOXED_ARTIFACT_BACKEND_PHI_REJECTED=YES", flush=True)


def test_artifact_id_mismatch_rejected(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    _write_payload(sandbox)
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox, artifact_id="artifact-other-id").resolve(_OPAQUE_REF, 1)
    assert caught.value.error_code == ERROR_ARTIFACT_ID_MISMATCH


def test_relative_sandbox_root_rejected(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.chdir(tmp_path)
    relative_root = Path("relative-sandbox")
    relative_root.mkdir()
    with pytest.raises(ArtifactResolutionError) as caught:
        SandboxedFilesystemArtifactPort(relative_root, {})
    assert caught.value.error_code == ERROR_ARTIFACT_SANDBOX_ROOT_INVALID


def test_error_messages_do_not_leak_host_paths(tmp_path: Path) -> None:
    sandbox = tmp_path / "sandbox"
    sandbox.mkdir()
    with pytest.raises(ArtifactResolutionError) as caught:
        _port(sandbox).resolve(_OPAQUE_REF, 1)
    serialized = str(caught.value).replace("\\", "/")
    assert sandbox.as_posix() not in serialized
    assert "payload.bin" not in serialized
    print("SANDBOXED_ARTIFACT_BACKEND_PROTOCOL=PASS", flush=True)
