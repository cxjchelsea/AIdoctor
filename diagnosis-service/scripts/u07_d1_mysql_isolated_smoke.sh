#!/usr/bin/env bash
# Local Docker only. Disposable synthetic MySQL, no company DB, PHI or Spring.
set -Eeuo pipefail
die() { echo "NOT_RUN: $*" >&2; exit 2; }
pass() { echo "PASS $*"; }
command -v docker >/dev/null 2>&1 || die "docker absent"
command -v python3 >/dev/null 2>&1 || die "python3 absent"
if [[ -n "$(printenv DOCKER_HOST || true)" || -n "$(printenv DOCKER_CONTEXT || true)" ]]; then
  die "Docker overrides forbidden"
fi
[[ "$(docker context show)" == default ]] || die "non-default Docker context forbidden"
endpoint="$(docker context inspect default --format '{{ .Endpoints.docker.Host }}' 2>/dev/null)" || die "cannot inspect local Docker endpoint"
[[ "$endpoint" == unix://* ]] || die "remote Docker endpoint forbidden"
docker info >/dev/null 2>&1 || die "Docker daemon unavailable"
MYSQL_IMAGE=mysql:8.0
FLYWAY_IMAGE=flyway/flyway:9.22.3
for img in "$MYSQL_IMAGE" "$FLYWAY_IMAGE"; do
  docker image inspect "$img" >/dev/null 2>&1 || die "pre-pull $img into trusted local Docker"
done
here="$(cd "$(dirname "$0")" && pwd -P)"
migrations="$(cd "$here/../src/main/resources/db/migration" && pwd -P)"
python3 "$here/verify_u07_d1_schema.py" || die "static migration parity failed"
pass "static schema test"
random="$(python3 -c 'import secrets; print(secrets.token_hex(6))')"
pw="$(python3 -c 'import secrets; print(secrets.token_hex(24))')"
net="u07_d1_net_$random"
dbcontainer="u07_d1_mysql_$random"
db=u07_d1_smoke
username=u07_d1_local
network_created=0
container_created=0
cleanup() {
  set +e
  if [[ "$container_created" == 1 ]]; then docker rm -f "$dbcontainer" >/dev/null 2>&1; fi
  if [[ "$network_created" == 1 ]]; then docker network rm "$net" >/dev/null 2>&1; fi
}
trap cleanup EXIT
docker network create --internal --label org.aidoctor.test=u07-d1 "$net" >/dev/null
network_created=1
docker run -d --pull never --name "$dbcontainer" --network "$net" --network-alias u07-d1-db \
  --label org.aidoctor.test=u07-d1 \
  -e MYSQL_ROOT_PASSWORD="$pw" -e MYSQL_DATABASE="$db" \
  -e MYSQL_USER="$username" -e MYSQL_PASSWORD="$pw" \
  "$MYSQL_IMAGE" --default-authentication-plugin=mysql_native_password >/dev/null
container_created=1
sql() {
  docker exec -e MYSQL_PWD="$pw" "$dbcontainer" mysql -u "$username" -D "$db" \
    -N -B -s -e "$1"
}
ready=0
for ((i=0;i<90;i++)); do
  if sql "SELECT 1" >/dev/null 2>&1; then ready=1; break; fi
  sleep 2
done
[[ "$ready" == 1 ]] || die "local MySQL failed to initialize"
flyway() {
  docker run --rm --pull never --network "$net" -v "$migrations:/flyway/sql:ro" \
    -e "FLYWAY_URL=jdbc:mysql://u07-d1-db:3306/$db?allowPublicKeyRetrieval=true&useSSL=false" \
    -e "FLYWAY_USER=$username" -e "FLYWAY_PASSWORD=$pw" \
    "$FLYWAY_IMAGE" -locations=filesystem:/flyway/sql "$@"
}
flyway migrate; pass "V1-V7 on isolated MySQL"
flyway validate; pass "Flyway validate"
flyway migrate; pass "repeat migrate"
history="$(sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1")"
[[ "$history" == 7 ]] || die "expected seven successful migrations, got $history"
pass "seven recorded migrations"
for table in u07_event_application u07_effect_outbox; do
  actual="$(sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='$table'")"
  [[ "$actual" == 1 ]] || die "missing table $table"
done
pass "both U07 tables"
insert_application() {
  local id="$1" effect="$2" value=NULL
  [[ -z "$effect" ]] || value="'$effect'"
  sql "INSERT INTO u07_event_application
  (event_id,consultation_id,question_id,parent_wait_effect_id,payload_digest,
  source_event_ref,source_version_ref,source_state_version,phase,row_version,
  effect_id,created_at,updated_at)
  VALUES ('$id','synthetic-consult','synthetic-question','synthetic-wait',
  'synthetic-digest','synthetic-source','synthetic-v1',1,'RECEIVED',0,$value,NOW(),NOW())" >/dev/null
}
insert_application synthetic-null-1 ""
insert_application synthetic-null-2 ""
pass "nullable unique effect permits multiple NULL"
insert_application synthetic-effect-1 synthetic-effect
if insert_application synthetic-effect-2 synthetic-effect >/dev/null 2>&1; then
  echo "FAIL duplicate non-NULL effect allowed" >&2; exit 1
fi
pass "non-NULL duplicate effect denied"
sql "INSERT INTO u07_effect_outbox
 (effect_id,event_id,target_type,payload_ref_or_digest,effect_status,attempt,created_at,updated_at)
 VALUES('outbox-1','synthetic-effect-1','SYNTHETIC','digest','PENDING',0,NOW(),NOW())" >/dev/null
if sql "INSERT INTO u07_effect_outbox
 (effect_id,event_id,target_type,payload_ref_or_digest,effect_status,attempt,created_at,updated_at)
 VALUES('outbox-1','synthetic-effect-1','SYNTHETIC','digest','PENDING',0,NOW(),NOW())" >/dev/null 2>&1; then
  echo "FAIL duplicate outbox primary key allowed" >&2; exit 1
fi
pass "outbox primary key enforced"
sql "START TRANSACTION;
 INSERT INTO u07_effect_outbox
 (effect_id,event_id,target_type,payload_ref_or_digest,effect_status,attempt,created_at,updated_at)
 VALUES('outbox-rollback','synthetic-effect-1','SYNTHETIC','digest','PENDING',0,NOW(),NOW());
 ROLLBACK;" >/dev/null
count="$(sql "SELECT COUNT(*) FROM u07_effect_outbox WHERE effect_id='outbox-rollback'")"
[[ "$count" == 0 ]] || die "DML rollback failed"
pass "synthetic DML rollback (not DDL rollback)"
echo "U07_D1_MYSQL_ENGINE_SMOKE=PASS"
