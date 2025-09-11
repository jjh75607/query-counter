# CHANGELOG

모든 중요한 변경 사항은 이 파일에 기록됩니다.  
형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),  
버전 관리 체계는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

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

