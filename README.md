# light-workflow

Java 21, Gradle 8, Spring Boot, MyBatis 기반의 기본 프로젝트입니다.

## 실행 환경

- Java 21
- Gradle 8.14.3 (Gradle Wrapper 포함)
- Spring Boot 3.4.5
- MyBatis Spring Boot Starter 3.0.4
- H2 Database

## 실행

```bash
./gradlew bootRun
```

애플리케이션은 `http://localhost:8080`에서 실행됩니다. 개발 중 H2 콘솔은
`http://localhost:8080/h2-console`에서 사용할 수 있습니다.

## 검증

```bash
./gradlew clean test
```

MyBatis XML 매퍼는 `src/main/resources/mapper` 아래에 두며,
`application.yml`의 `mybatis.mapper-locations` 설정으로 로드됩니다.

## Watchlist 검색

Watchlist 원천 데이터는 DB에서 조회하고, Lucene 인덱스는
`WATCHLIST_INDEX_PATH`로 지정한 파일 시스템 경로에 영속 저장합니다.
여러 서버가 같은 인덱스를 사용하려면 해당 경로가 모든 서버에서 접근 가능한
공유 파일 시스템이어야 합니다. Lucene의 파일 잠금으로 동시 쓰기는 보호되지만,
대규모 운영에서는 색인 갱신을 한 대의 전용 작업 노드로 단일화하는 것을 권장합니다.

기존 DB 데이터를 인덱싱하거나 전체 재동기화할 때는 다음 API를 호출합니다.

```text
POST /api/watchlist/rebuild
```

DB Mapper는 `watchlist_entry` 테이블과 다음 컬럼을 기준으로 작성되어 있으므로,
실제 AML 스키마가 다르면 `WatchlistEntryMapper.xml`의 SQL을 맞춰야 합니다.

```bash
curl -X POST http://localhost:8080/api/watchlist/entries \
  -H 'Content-Type: application/json' \
  -d '{"id":"wl-1","koreanName":"홍길동","englishName":"Hong Gil Dong","dateOfBirth":"1980-01-02","country":"KR","residence":"대한민국","aka":["Hong Gildong"],"gender":"M","listingReason":"금융제재"}'

curl -X POST http://localhost:8080/api/watchlist/search \
  -H 'Content-Type: application/json' \
  -d '{"englishName":"Hong Gildong","country":"KR"}'
```

검색 요청은 한글명, 영문명, 생년월일, 국가, 거주지, AKA, 성별, 등재 사유를
각각 검색할 수 있습니다. `src/main/resources/application.yml`의
`watchlist.search.field-weights`에서 항목별 가중치를 조정하며, 결과 점수는
해당 검색 결과 중 최고 점수를 100점으로 정규화합니다. 이름·AKA·등재 사유는
다국어 분석과 오타 허용 검색을 사용하고, 생년월일·국가·거주지·성별은
정확값을 기준으로 검색합니다.

검색 결과에는 `matchedFields`와 `riskLevel`이 함께 반환됩니다. 위험등급은
`watchlist.search.high-risk-threshold` 이상이면 `HIGH`, `review-threshold`
이상이면 `REVIEW`, 그 미만이면 `LOW`입니다. 검색 실행 시 요청 필드 수와
후보 수만 감사 로그로 기록하며, 개인정보인 검색어 원문은 로그에 남기지 않습니다.
