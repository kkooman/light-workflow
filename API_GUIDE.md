# light-workflow API 가이드

## 1. 기본 정보

- 기본 주소: `http://localhost:8080`
- API 응답 시각: UTC ISO-8601 형식
- 애플리케이션 로직 시간대: `Asia/Seoul`
- Swagger UI: [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [`/v3/api-docs`](http://localhost:8080/v3/api-docs)

## 2. 공통 응답 형식

대부분의 JSON API는 다음 형식으로 응답합니다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청이 정상 처리되었습니다.",
  "data": {},
  "timestamp": "2026-09-09T07:51:30Z"
}
```

| 필드 | 설명 |
| --- | --- |
| `success` | 처리 성공 여부 |
| `code` | 응답 코드 |
| `message` | 사용자에게 전달할 메시지 |
| `data` | 실제 응답 데이터. 실패 시 `null` |
| `timestamp` | 응답 시각(UTC) |

## 3. 인증

### 로그인

`POST /api/auth/login`

인증에 성공하면 JWT를 발급하고 토큰 저장소에 등록합니다.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'
```

응답 예시:

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "로그인 성공",
  "data": {
    "accessToken": "<JWT>"
  },
  "timestamp": "2026-09-09T07:51:30Z"
}
```

인증이 필요한 API에는 다음 헤더를 사용합니다.

```http
Authorization: Bearer <JWT>
```

현재 로컬 기본 계정은 `admin`/`admin`이며, 운영 환경에서는 반드시 실제 사용자 저장소와 비밀번호 정책으로 교체해야 합니다.

## 4. 상태 확인 API

상태 확인 API는 로드밸런서와 배포 자동화에서 사용할 수 있습니다. 점검 마커 파일이 존재하면 HTTP `503 Service Unavailable`을 반환합니다.

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/health` | 불필요 | JSON 상태 확인 |
| `GET` | `/api/status` | 불필요 | `/api/health`와 동일한 JSON 상태 확인 |
| `GET` | `/status.html` | 불필요 | HTML 상태 확인 |

정상 응답의 `data` 예시:

```json
{
  "status": "UP",
  "maintenance": false,
  "service": "light-workflow"
}
```

## 5. 공통코드 API

| 메서드 | 경로 | 인증 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/common-codes` | 불필요 | 검색 가능한 모든 enum 공통코드 조회 |
| `GET` | `/api/common-codes/{enumName}` | 불필요 | 특정 enum 공통코드 조회 |

특정 enum 응답의 `data` 예시:

```json
[
  {
    "code": "HIGH",
    "label": "고위험"
  }
]
```

존재하지 않는 enum은 HTTP `404`와 `COMMON_CODE_NOT_FOUND`를 반환합니다.

## 6. Watchlist API

모든 Watchlist API의 기본 경로는 `/api/watchlist`입니다.

### 항목 저장

`POST /api/watchlist/entries`

인증이 필요합니다. `id`는 필수이며, 저장 후 DB와 Lucene 색인을 갱신합니다.

```bash
curl -X POST http://localhost:8080/api/watchlist/entries \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{
    "id": "wl-1",
    "koreanName": "홍길동",
    "englishName": "Hong Gil Dong",
    "dateOfBirth": "1980-01-02",
    "country": "KR",
    "residence": "대한민국",
    "aka": ["Hong Gildong"],
    "gender": "M",
    "listingReason": "금융제재"
  }'
```

### 항목 삭제

`DELETE /api/watchlist/entries/{id}`

인증이 필요합니다.

### 검색

`POST /api/watchlist/search`

인증이 필요합니다. 다음 검색 필드를 조합할 수 있습니다.

`koreanName`, `englishName`, `dateOfBirth`, `country`, `residence`, `aka`, `gender`, `listingReason`

```bash
curl -X POST http://localhost:8080/api/watchlist/search \
  -H 'Authorization: Bearer <JWT>' \
  -H 'Content-Type: application/json' \
  -d '{"englishName":"Hong Gildong","country":"KR"}'
```

검색 결과에는 매칭 필드와 위험등급이 포함됩니다. 검색 결과가 검출되면 검출 기록을 저장하고 전자결재 건을 상신합니다.

### 색인 전체 재구성

`POST /api/watchlist/rebuild`

`SYSTEM_ADMIN` 역할만 실행할 수 있습니다.

### 색인 증분 동기화

`POST /api/watchlist/sync`

`SYSTEM_ADMIN` 역할만 실행할 수 있습니다.

요청 예시:

```json
{
  "ids": ["wl-1", "wl-2"]
}
```

### 색인 상태 조회

`GET /api/watchlist/index/status`

인증이 필요합니다.

## 7. 전자결재 API

결재 순서는 다음과 같습니다.

`상신자 → AML 담당자 → AML 책임자`

AML 담당자는 개인 또는 부서로 지정할 수 있습니다. 부서 결재 시 요청 헤더에 `X-Department`를 전달합니다.

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| `GET` | `/api/approvals/{approvalId}` | 로그인 사용자 | 결재 건 조회 |
| `GET` | `/api/approvals/by-detection/{detectionId}` | 로그인 사용자 | 검출 건으로 결재 건 조회 |
| `POST` | `/api/approvals/{approvalId}/submit` | `SYSTEM_ADMIN` 또는 `AML_SUBMITTER` | 상신자 결재 |
| `POST` | `/api/approvals/{approvalId}/aml-officer/approve` | `SYSTEM_ADMIN` 또는 `AML_OFFICER` | AML 담당자 결재 |
| `POST` | `/api/approvals/{approvalId}/aml-manager/approve` | `SYSTEM_ADMIN` 또는 `AML_MANAGER` | AML 책임자 결재 |

AML 담당자가 부서 대상인 경우:

```bash
curl -X POST http://localhost:8080/api/approvals/{approvalId}/aml-officer/approve \
  -H 'Authorization: Bearer <JWT>' \
  -H 'X-Department: AML_REVIEW'
```

결재 순서를 건너뛰거나 대상자가 일치하지 않으면 요청이 거부됩니다.

## 8. 주요 오류 코드

| HTTP 상태 | 코드 | 설명 |
| --- | --- | --- |
| `401` | `AUTH_FAILED` | 아이디 또는 비밀번호가 올바르지 않음 |
| `401` | - | JWT가 없거나 유효하지 않음 |
| `403` | - | API 실행 권한 부족 |
| `404` | `COMMON_CODE_NOT_FOUND` | 요청한 공통코드가 없음 |
| `503` | `MAINTENANCE` | 서비스 점검 중 |

## 9. 테스트

```bash
./gradlew clean test --rerun-tasks
```
