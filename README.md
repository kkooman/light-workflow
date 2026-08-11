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

Watchlist 데이터는 현재 애플리케이션 메모리의 Lucene 인덱스에 저장됩니다.
운영 환경에서는 데이터 저장소와 인덱스 재구축 작업을 연결해야 합니다.

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
