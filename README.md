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
