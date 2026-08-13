# CHANGELOG

모든 중요한 변경 사항은 이 파일에 기록됩니다.  
형식은 [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),  
버전 관리 체계는 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

---

## [Unreleased]
### Changed
- **Maven Central 에서 받을 수 있습니다. 설치 좌표가 `io.github.jjh75607:query-counter:0.2.0` 입니다.** 종전에는 빌드 스크립트에 `maven { url 'https://jitpack.io' }` 를 추가해야 했지만 이제 `mavenCentral()` 만 있으면 됩니다. JitPack 도 계속 지원하며 그쪽 좌표는 `com.github.jjh75607:query-counter:v0.2.0` 로 종전과 같습니다. **같은 라이브러리인데 받는 경로에 따라 groupId 가 다릅니다.** JitPack 이 빌드 파일의 `group` 값을 무시하고 항상 `com.github.<계정>` 으로 서빙하기 때문이며, 남의 네임스페이스로 라이브러리가 올라가는 것을 막으려는 설계입니다. `v0.1.0` 까지의 릴리스는 JitPack 에서만 받을 수 있습니다

### Added
- Maven Central 배포 워크플로. GitHub 릴리스를 만들면 아티팩트를 서명해 Central 에 올립니다. 태그와 `build.gradle` 의 `version` 이 다르면 올리기 전에 멈춥니다. Central 은 한 번 올라간 버전을 덮어쓸 수 없기 때문입니다
- 릴리스 검증 워크플로에 Central 확인을 추가했습니다. POM 좌표, 자동 설정 등록 파일, 서명과 sources, javadoc 이 실제로 올라갔는지 봅니다. Central 은 공개를 손으로 눌러야 하고 반영에 시간이 걸리므로 이 확인은 수동 실행일 때만 돕니다
- **쿼리 수 상한 검증을 추가했습니다.** `select(atMost(3))` 처럼 쓰면 3개 이하일 때 통과합니다. `select`, `insert`, `update`, `delete`, `others` 다섯 메서드와 테이블별 검증 모두에서 됩니다. 실무에서는 정확한 쿼리 수보다 상한이 더 자주 필요하고, 구현 세부가 조금 바뀌어도 통과해야 하는 테스트가 많기 때문입니다. 숫자를 그대로 넘기면 종전처럼 정확한 값 검증이라 기존 코드는 그대로 동작합니다. 실패 메시지는 `expected at most 3, but was 5` 로 비교 방식을 함께 밝힙니다
- CI 워크플로. `master` push 와 PR 에서 Spring Boot 하한(3.0.0)과 기본값 두 조합으로 `./gradlew build` 를 돌립니다
- 릴리스 검증 워크플로. 릴리스 직후 JitPack 빌드 상태와 산출물, 자동 설정 등록 파일 포함 여부를 확인합니다
- README 에 요구 사항 표 추가. Java 17 이상, Spring Boot 3.0 이상이며 4.x 도 지원합니다. CI 가 검증하는 범위와 일치시켰습니다
- 돌아가는 예제 패키지. `src/test/java/soon/springtestutil/example` 에 JPA 엔티티와 사용 예제 테스트를 두어 README 예제가 실제로 컴파일되고 실행되는지 검증합니다
- Spotless 로 포맷 검사. 사용하지 않는 import 제거, 뒤 공백 제거, 파일 끝 개행, 들여쓰기 4칸 스페이스만 검사하는 설정입니다. `spotlessCheck` 가 `check` 에 물려 있어 `./gradlew build` 와 CI 가 함께 검사합니다. 라이브러리 동작에는 영향이 없습니다
- **JDBC 배치가 1건으로 집계된다는 설명을 README 두 벌에 추가했습니다.** `addBatch` 로 쌓고 `executeBatch` 로 보내면 몇 건을 쌓았든 1건입니다. 데이터베이스 왕복이 한 번이기 때문입니다. `hibernate.jdbc.batch_size` 를 켠 프로젝트에서 엔티티 10건을 저장해도 카운트가 1이 되므로 라이브러리가 쿼리를 놓쳤다고 오해하기 쉬웠습니다. 이 동작을 고정하는 테스트도 함께 두었습니다. 집계 방식 자체는 바뀌지 않았습니다

### Fixed
- **H2 의 데이터 변경 델타 테이블 문장에서 INSERT 가 세지지 않고 테이블 이름이 `final` 로 뽑히던 문제를 고쳤습니다.** H2 는 자동 증가 키를 돌려줄 때 `select ID from final table (insert into member ...)` 형태를 보내는데, 맨 앞 키워드로 판정해 `SELECT` 1건이 되고 `insert(1)` 을 기대한 테스트가 0건으로 실패했습니다. 왕복이 1회이고 사용자가 의도한 동작은 저장이므로 안쪽 문장의 타입으로 1건을 셉니다. `final table`, `new table`, `old table` 세 형태를 모두 다루며, 테이블 이름도 델타 키워드가 아니라 대상 테이블로 뽑습니다
- **앞에 주석이 붙거나 CTE 로 시작하는 SQL 이 모두 `OTHERS` 로 분류되던 문제를 고쳤습니다.** 판정 전에 선행 주석(`/* */` 와 `--`)을 벗겨내고, `WITH` 로 시작하면 괄호 밖의 첫 키워드로 판정합니다. `hibernate.use_sql_comments=true` 를 켠 프로젝트는 Hibernate 가 모든 SQL 앞에 주석을 붙이므로 모든 쿼리가 `OTHERS` 가 되어 `select(n)` 을 쓰는 테스트가 전부 0건으로 보였습니다
- **DataSource 빈이 다른 DataSource 빈을 위임하면 쿼리가 두 번 세지던 문제를 고쳤습니다.** `LazyConnectionDataSourceProxy` 처럼 다른 DataSource 를 감싸는 빈이 있으면 안쪽과 바깥쪽이 모두 감싸져 같은 쿼리가 두 번 기록됐습니다. 위임 대상이 이미 감싸져 있으면 바깥쪽은 감싸지 않도록 했습니다. 서로 위임하지 않는 DataSource 가 둘인 구성에서는 종전대로 양쪽 모두 기록됩니다
- **DataSource 에 의존하는 다른 `BeanPostProcessor` 가 있으면 쿼리가 하나도 기록되지 않던 문제를 고쳤습니다.** `Ordered` 를 구현하면서 DataSource 를 주입받는 `BeanPostProcessor` 가 있으면 그것을 만드는 과정에서 DataSource 가 먼저 만들어져 감싸지지 않았습니다. 이 라이브러리의 `BeanPostProcessor` 가 `PriorityOrdered` 를 구현하도록 바꿔 항상 먼저 등록되게 했습니다. 증상이 카운트 불일치가 아니라 0건이었고 안내 문구도 원인과 어긋나 있어 찾기 어려운 문제였습니다
- 자동 설정 테스트가 Spring Boot 의 `DataSourceAutoConfiguration` 에 묶여 있어 Spring Boot 4 에서 컴파일되지 않았습니다. 테스트용 DataSource 를 직접 등록하도록 바꿔 버전에 묶이지 않게 했습니다. 라이브러리 본체는 4.x 에서 원래 정상이었습니다
- 문서에 셋업 쿼리도 카운트에 포함된다는 설명을 추가했습니다. 기록은 테스트 메서드 직전에 초기화되므로 `@BeforeEach` 의 쿼리가 함께 세집니다

## [0.1.0] - 2026-08-06
### Added
- `@ExtendWith(QueryCountTestExtension.class)` 없이도 동작합니다. `TestExecutionListener` 를 `META-INF/spring.factories` 에 등록해 모든 Spring 테스트가 자동으로 집어갑니다
    - 사용자 쪽 설정이 필요 없습니다
    - Spring 테스트 컨텍스트를 띄우지 않는 테스트에서는 기존처럼 `@ExtendWith` 를 씁니다
- **`verify()` 가 선택이 되었습니다.** 만들어두고 검증하지 않은 어서션은 테스트가 끝날 때 자동으로 검증됩니다
    - 이전에는 `verify()` 를 잊으면 테스트가 조용히 통과했습니다
    - 테스트가 다른 이유로 이미 실패했으면 자동 검증을 건너뜁니다. 원래 실패 원인이 가려지지 않습니다

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

