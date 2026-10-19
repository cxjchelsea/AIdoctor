#!/usr/bin/env python3
"""Run the isolated, nonproduction V1 two-turn HTTP host on Windows/Linux.

Creates its OWN disposable MySQL container, reproduces CI fixture schema,
then launches the TEST-classpath synthetic Spring host on 127.0.0.1 (configurable port).
No connection to existing DBs and no production credentials are accepted.
"""
import argparse
import os
import pathlib
import shutil
import subprocess
import sys
import textwrap
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
NAME = "aidoctor-v1-synthetic-manual-mysql"
PASSWORD = "synthetic-u01-u02-root"


def execute(command, *, input_bytes=None, env=None):
    return subprocess.run(command, cwd=ROOT, input=input_bytes, check=True, env=env)


def apply_sql(sql):
    execute(["docker", "exec", "-i", "-e", "MYSQL_PWD=" + PASSWORD,
             NAME, "mysql", "-uroot", "v1_u01_u02"],
            input_bytes=sql.encode("utf-8"))


def fixture_blocks():
    workflow = (ROOT / ".github/workflows/v1-u01-u02-physical-mysql.yml").read_text(
        encoding="utf-8")
    blocks = []
    lines = workflow.splitlines()
    in_block = False
    current = []
    for line in lines:
        if "<<'SQL'" in line and not in_block:
            in_block = True
            current = []
        elif in_block and line.strip() == "SQL":
            blocks.append(textwrap.dedent("\n".join(current)) + "\n")
            in_block = False
        elif in_block:
            current.append(line)
    if in_block or len(blocks) != 2:
        raise RuntimeError("Expected exactly two pinned SQL fixture blocks in workflow")
    return blocks


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accept-disposable-db", action="store_true",
                        help="I understand this starts a new synthetic MySQL container")
    parser.add_argument("--mysql-port", type=int, default=34061,
                        help="Loopback-only disposable MySQL host port (default: 34061)")
    parser.add_argument("--http-port", type=int, default=18084,
                        help="Loopback-only HTTP port (default: 18084)")
    args = parser.parse_args()
    if not 1024 <= args.http_port <= 65535:
        parser.error("--http-port must be within 1024..65535")
    if not 1024 <= args.mysql_port <= 65535:
        parser.error("--mysql-port must be within 1024..65535")
    jdbc = (f"jdbc:mysql://127.0.0.1:{args.mysql_port}/v1_u01_u02?"
            "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")
    if not args.accept_disposable_db:
        parser.error("Explicit --accept-disposable-db opt-in required")
    if os.environ.get("GITHUB_ACTIONS") == "true":
        parser.error("This launcher is for local machines, not CI")
    env = os.environ.copy()
    env.update({
        "V1_LOCAL_SYNTHETIC_HOST": "LOCAL_SYNTHETIC_ONLY",
        "V1_U01_U02_MYSQL_MODE": "DISPOSABLE_U01_U02_ONLY",
        "V1_U01_U02_MYSQL_URL": jdbc,
        "V1_LOCAL_SYNTHETIC_HTTP_PORT": str(args.http_port),
    })
    mvn = shutil.which("mvn")
    if not mvn:
        parser.error("Java 8 Maven (mvn.cmd on Windows) must be available in PATH")
    started = False
    try:
        execute(["docker", "run", "--rm", "-d", "--name", NAME,
                 "-e", "MYSQL_ROOT_PASSWORD=" + PASSWORD,
                 "-e", "MYSQL_DATABASE=v1_u01_u02",
                 "-p", f"127.0.0.1:{args.mysql_port}:3306", "mysql:8.0"])
        started = True
        for _ in range(60):
            ready = subprocess.run(
                ["docker", "exec", "-e", "MYSQL_PWD=" + PASSWORD, NAME,
                 "mysqladmin", "ping", "-h127.0.0.1", "-uroot"],
                cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if ready.returncode == 0:
                break
            time.sleep(2)
        else:
            raise RuntimeError("Disposable MySQL container did not become healthy")
        first, second = fixture_blocks()
        apply_sql(first)
        for filename in (
            "diagnosis-service/src/main/resources/db/migration/V2__create_clinical_runtime_foundation.sql",
            "diagnosis-service/src/main/resources/db/migration/V3__create_clinical_consultation.sql",
        ):
            apply_sql((ROOT / filename).read_text(encoding="utf-8"))
        apply_sql(second)
        for filename in (
            "diagnosis-service/src/main/resources/db/migration/V4__create_capability_governance_foundation.sql",
            "diagnosis-service/src/main/resources/db/migration/V5__extend_capability_trace.sql",
            "tools/u07_d5_o2_tx_topology/production_candidate/mysql8/V9__u07_clinical_receipts_no_dispatch.sql",
        ):
            apply_sql((ROOT / filename).read_text(encoding="utf-8"))
        execute([mvn, "-B", "-ntp", "-f", "contracts/v1/bindings/java/pom.xml",
                 "install", "-DskipTests"], env=env)
        execute([mvn, "-B", "-ntp", "-f", "diagnosis-service/pom.xml",
                 "test-compile", "-DskipTests"], env=env)
        print(f"Synthetic HTTP service: http://127.0.0.1:{args.http_port}", flush=True)
        print(f"Frontend: cd frontend ; $env:V1_CLINICAL_HTTP_PORT={args.http_port} ; npm ci ; npm run dev", flush=True)
        print("Open http://localhost:3000/test/v1-clinical", flush=True)
        execute([mvn, "-B", "-ntp", "-f", "diagnosis-service/pom.xml",
                 "org.codehaus.mojo:exec-maven-plugin:3.1.0:java",
                 "-Dexec.mainClass=com.aidoctor.diagnosis.runtime.u02.V1LocalSyntheticManualServer",
                 "-Dexec.classpathScope=test"], env=env)
    except KeyboardInterrupt:
        print("Stopping isolated synthetic host.", flush=True)
    finally:
        if started:
            subprocess.run(["docker", "rm", "-f", NAME], cwd=ROOT, check=False,
                           stdout=subprocess.DEVNULL)


if __name__ == "__main__":
    main()
