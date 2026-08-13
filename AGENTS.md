# AML Project - AI Development Guidelines

## Purpose

This document defines the common development rules for AI coding agents working on this AML project.

> 이 문서는 AML 프로젝트에서 AI Coding Agent가 따라야 하는 공통 개발 규칙을 정의한다.

## Technology Stack

- Java 21
- Spring Boot 3.x
- MyBatis
- MySQL
- Gradle

## Core Principles

1. Analyze existing code before making changes.
   > 코드를 수정하기 전에 기존 구현을 먼저 분석한다.

2. Follow the existing project architecture and conventions.
   > 기존 프로젝트의 아키텍처와 코딩 규칙을 우선적으로 따른다.

3. Do not introduce unnecessary design patterns or abstractions.
   > 필요하지 않은 디자인 패턴이나 추상화를 임의로 도입하지 않는다.

4. Keep business logic explicit and maintainable.
   > 비즈니스 로직은 명확하고 유지보수하기 쉬운 Java 코드로 표현한다.

5. Minimize the scope of changes.
   > 요청하지 않은 대규모 리팩터링이나 변경을 하지 않는다.

## Architecture

Use clear separation of responsibilities:

- Controller: HTTP request/response handling
- Service: Business logic
- Mapper/Repository: Data access
- Domain/DTO: Domain and data representation

> Controller는 요청/응답 처리, Service는 비즈니스 로직, Mapper/Repository는 데이터 접근을 담당한다.

## Database and SQL

SQL should primarily handle data access.

Avoid placing complex business rules in SQL.

Examples of logic that should generally remain in Java:

- Customer-type-specific business rules
- AML decision rules
- Risk score calculation
- Complex state transitions
- Multi-step business processes

> SQL에 복잡한 업무 로직을 넣기보다 Java 애플리케이션 계층에서 명시적으로 표현한다.

## AML Domain

Supported customer types:

- Individual (개인)
- Sole Proprietor (개인사업자)
- Corporation (법인)
- Non-Profit Corporation (비영리법인)

Major AML domains include:

- KYC (고객확인)
- Customer Risk Assessment (고객위험평가)
- Watchlist Screening (요주의리스트 스크리닝)
- PEP (정치적 주요인물)
- Sanctions (제재)
- Adverse Media (부정적 언론정보)
- Transaction Monitoring (거래모니터링)
- STR / Suspicious Transaction Report (의심거래보고)
- Audit Trail (감사 추적)
- Reporting (보고)

## Auditability

AML decisions and important state changes should be traceable.

Where applicable, preserve:

- Decision reason
- Input data
- Rule or condition used
- Score calculation rationale
- Previous state
- New state
- Actor
- Timestamp

> AML 판단과 상태 변경은 가능한 경우 사후에 재현하고 감사할 수 있어야 한다.

## Security

- Never log sensitive personal information unnecessarily.
- Do not expose credentials, tokens, passwords, or secrets.
- Validate external input.
- Consider authorization requirements.
- Avoid SQL injection vulnerabilities.

> 개인정보 및 민감정보는 로그에 불필요하게 출력하지 않는다.

## Testing

When changing behavior:

- Add or update relevant tests.
- Cover normal cases.
- Cover exception cases.
- Cover boundary values.
- Cover customer-type differences.
- Cover state transitions where applicable.

> 기능 변경 시 관련 테스트를 추가하거나 수정한다.

## Change Policy

Before making a large change:

1. Understand the existing implementation.
2. Identify the smallest safe change.
3. Explain significant architectural changes.
4. Avoid unrelated refactoring.

> 변경 범위를 최소화하고 요청하지 않은 영역은 임의로 변경하지 않는다.
