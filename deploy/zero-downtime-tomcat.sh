#!/usr/bin/env bash
set -Eeuo pipefail

TOMCAT_HOME="${TOMCAT_HOME:-/opt/tomcat}"
APP_CONTEXT="${APP_CONTEXT:-ROOT}"
WAR_PATH="${WAR_PATH:-}"
STATUS_URL="${STATUS_URL:-http://127.0.0.1:8080/status.html}"
LB_STATUS_URL="${LB_STATUS_URL:-${STATUS_URL}}"
DELAY_SECONDS="${DELAY_SECONDS:-60}"
MAINTENANCE_FILE="${MAINTENANCE_FILE:-/tmp/lightworkflow-maintenance.marker}"
APP_BASE="${TOMCAT_HOME}/webapps"
APP_WAR="${APP_BASE}/${APP_CONTEXT}.war"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"
}

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    log "필수 파일이 없습니다: $path"
    exit 1
  fi
}

if [[ -z "$WAR_PATH" ]]; then
  log "WAR_PATH가 비어 있습니다. 예: WAR_PATH=/tmp/app.war ./deploy/zero-downtime-tomcat.sh"
  exit 1
fi
require_file "$WAR_PATH"

# LB가 /status.html을 호출하도록 인프라팀이 설정해 두었다면,
# 이 파일 마커를 통해 앱이 503을 반환하도록 만든다.
# 이후 LB가 해당 응답을 보게 되면 멤버를 제외한 뒤 서버를 종료한다.
touch "$MAINTENANCE_FILE"

status_code=""
for _ in $(seq 1 60); do
  status_code="$(curl -sS -o /tmp/zero-downtime-status.html -w '%{http_code}' "$LB_STATUS_URL" || true)"
  log "LB status check: $status_code"
  if [[ "$status_code" != "200" ]]; then
    break
  fi
  sleep 2
done

if [[ "$status_code" == "200" || "$status_code" == "000" ]]; then
  log "LB가 아직 200을 보고 있습니다. status.html이 503을 반환하도록 인프라/앱 상태를 확인하세요."
  exit 1
fi

log "Tomcat shutdown 시작"
if [[ -x "${TOMCAT_HOME}/bin/shutdown.sh" ]]; then
  "${TOMCAT_HOME}/bin/shutdown.sh" || true
else
  log "${TOMCAT_HOME}/bin/shutdown.sh 를 찾지 못했습니다."
  exit 1
fi
sleep 15

log "배포 전 ${DELAY_SECONDS}초 대기"
sleep "$DELAY_SECONDS"

log "새 버전 배포"
cp "$WAR_PATH" "$APP_WAR"

log "Tomcat startup 시작"
"${TOMCAT_HOME}/bin/startup.sh"

for _ in $(seq 1 60); do
  status_code="$(curl -sS -o /tmp/zero-downtime-post-start.html -w '%{http_code}' "$LB_STATUS_URL" || true)"
  log "배포 후 LB status check: $status_code"
  if [[ "$status_code" == "200" ]]; then
    break
  fi
  sleep 2
done

rm -f "$MAINTENANCE_FILE"

log "배포 완료. 상태 확인: $LB_STATUS_URL"
