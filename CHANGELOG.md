# CHANGELOG

모든 중요한 변경 사항은 이 파일에 기록됩니다.  
형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),  
버전 관리 체계는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [Unreleased]
### Changed
- **활성화 방식이 바뀌었습니다.** 의존성만 추가하면 켜지던 것이 `query-counter.enabled=true` 를 설정해야 켜지도록 변경
    - 활성화하지 않으면 DataSource 를 감싸지 않으므로 이 라이브러리를 쓰지 않는 테스트에 영향을 주지 않습니다
    - 기존처럼 동작시키려면 테스트 설정에 `query-counter.enabled=true` 를 추가하세요
- SQL 로깅을 쿼리 카운팅과 분리했습니다. `query-counter.logging.enabled` 로 따로 켜며 기본값은 비활성입니다
    - 이전에는 항상 켜져 있어 테스트가 많은 프로젝트에서 로그가 오염됐습니다
- 자동 설정에서 `@ComponentScan` 을 제거하고 `@Bean` 으로 명시하도록 변경

### Added
- `query-counter.enabled`, `query-counter.logging.enabled` 프로퍼티
- 설정 메타데이터 생성. IDE 에서 프로퍼티 자동완성과 설명이 표시됩니다
- 검증이 실패했는데 기록된 쿼리가 하나도 없으면 활성화 설정을 확인하라는 안내를 실패 메시지에 덧붙입니다

## [0.0.6] - 2026-01-08
### Added
- 테이블별 개별 쿼리 카운트 검증 기능 추가 (`forTable`)
    - `forTable("member").insert(2).select(1)` 형태로 테이블마다 다른 검증 조건 설정 가능
    - 여러 테이블에 대해 체이닝으로 각각 다른 조건 설정 가능
    - 테이블별 `maxExecutionTimeMs` 설정 지원
- `TableQueryAssertion` 클래스 추가

## [0.0.5] - 2025-09-11
### Added
- 개별 쿼리 실행 시간(`maxExecutionTimeMs`)을 지정하여 시간 초과 쿼리에 대해 검증할 수 있는 기능 추가
- `forTables`와 `maxExecutionTimeMs`를 조합하여 특정 테이블의 쿼리만 시간 검증 가능
- 관련 통합 테스트 및 단위 테스트 작성


## [0.0.4] - 2025-09-08
### Fixed
- 여러 테스트를 연속 실행할 때 테스트별 쿼리 카운트 격리가 깨지는 문제 해결

## [0.0.3] - 2025-06-30
### Added
- 지정된 테이블에 대해서만 쿼리 수를 검증하는 기능 추가
- `README.md`, `LICENSE`, `CHANGELOG.md` 문서 최초 작성

## [0.0.2] - 2025-06-06
### Changed
- 명시하지 않은 쿼리는 검증 대상에서 제외하도록 변경

## [0.0.1] - 2025-06-06
### Added
- 어노테이션 기반 쿼리 카운팅 기능 초기 구현
- 테스트 시 쿼리 수 검증 기능 기본 동작 추가

