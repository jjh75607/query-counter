# query-counter

Hibernate로 실행되는 쿼리 수와 실행 시간을 테스트에서 검증하는 라이브러리.
JitPack으로 배포한다.

## 이 라이브러리가 반드시 지켜야 할 것

**활성화하지 않으면 사용자의 테스트에 아무 영향을 주지 않아야 한다.**

테스트 라이브러리이므로 이걸 붙였다는 이유로 남의 테스트가 깨지면 존재 의미가 없다.
이 성질은 문서가 아니라 `AutoConfigTest`가 보증한다. 자동 설정을 건드릴 때
그 테스트를 먼저 읽는다.

- `AutoConfig`는 `query-counter.enabled=true`일 때만 활성화된다. 기본값은 꺼짐
- SQL 로깅은 `query-counter.logging.enabled`로 따로 켠다. 카운팅과 무관한 기능이라
  기본값이 꺼짐이다. 항상 켜져 있으면 테스트가 많은 프로젝트에서 로그가 오염된다
- 자동 설정에서 `@ComponentScan`을 쓰지 않는다. 사용자의 스캔과 겹치고 순서에 따라
  결과가 달라진다. `@Bean`으로 명시한다

## 구조

```
config/
  AutoConfig                        조건부 자동 설정. @Bean 으로 명시
  QueryCounterProperties            yml 프로퍼티. IDE 자동완성 메타데이터의 출처
  DataSourceProxyBeanPostProcessor  DataSource 를 프록시로 감쌈
querycount/
  datasource/QueryCountListener     datasource-proxy 리스너. 여기서 기록이 시작된다
  context/QueryCountContext         ThreadLocal 수집소
  context/QueryInfo                 쿼리 하나의 값 객체. 테이블 이름을 정규식으로 추출
  assertion/QueryCounterAssertion   플루언트 빌더 (전체 대상)
  assertion/TableQueryAssertion     플루언트 빌더 (테이블별)
  assertion/QueryCountVerifier      실제 비교와 오류 메시지 조립
  extension/QueryCountTestExtension JUnit 확장. 테스트 전후 ThreadLocal 정리
```

흐름은 세 층이다. **수집(Listener, Context) → 명세(Assertion) → 검증(Verifier).**

## 주의할 지점

### BeanPostProcessor에 프로퍼티 빈을 주입하지 않는다

`BeanPostProcessor`는 일반 빈보다 먼저 만들어진다. `QueryCounterProperties`를
주입받으면 프로퍼티 빈이 너무 이르게 초기화되고 Spring이 경고를 낸다.
`AutoConfig`에서 `Environment`로 값을 읽어 생성자에 넘긴다.

### 활성 여부 판단에 애플리케이션 컨텍스트를 쓰지 않는다

`TestContext.getApplicationContext()`를 부르면 컨텍스트 로딩이 강제된다.
감싸기가 실제로 일어났는지를 신호로 쓴다 (`QueryCountContext.markActive()`).

### 알려진 빚

건드릴 일이 있으면 함께 정리한다. 지금 당장 급하지는 않다.

| 위치 | 내용 |
|---|---|
| `QueryCountContext` | `active` 플래그가 static 이고 리셋되지 않는다 |
| `QueryCountListener` | `queryTypeCache` 가 SQL 문자열 키의 static 맵인데 비워지지 않는다 |
| `QueryCountListener` | `elapsedMs` 가 실행 단위 값인데 배치의 모든 쿼리에 같은 값이 붙는다 |
| `QueryInfo` | 생성자가 테이블 이름을 항상 정규식으로 추출한다. 안 쓰는 경우에도 |
| `QueryCountVerifier` | 229줄에 private 메서드 13개. 검사를 하나 더 추가하기 전에 검사 단위를 인터페이스로 뽑는 편이 낫다 |
| `verify()` | 잊으면 테스트가 조용히 통과한다. `TestExecutionListener`로 자동 호출하는 방향으로 정리 예정 |

## 작업 규칙

**이슈, 브랜치, PR을 거친다. `master`에 직접 커밋하지 않는다.**

| 항목 | 규칙 |
|---|---|
| base 브랜치 | `master` (단일 트렁크) |
| 유지보수 브랜치 | 지금은 없다. 이미 나간 버전을 패치해야 할 때 `0.0.x` 형태로 만든다 |
| 브랜치 이름 | `<type>/#<이슈번호>` 예: `feat/#42`, `docs/#54` |
| 커밋과 PR 제목 | `[#이슈번호] Type: 한국어 설명` |
| Type | `Feat`, `Fix`, `Docs`, `Refactor`, `Chore`, `Setting`, `Test` |
| 머지 | 스쿼시. 제목에 ` (#PR번호)`가 붙는다 |
| 이슈 본문 | `.github/ISSUE_TEMPLATE/` 의 타입별 템플릿 |
| PR 본문 | `.github/PULL_REQUEST_TEMPLATE.md` |

## 테스트 규칙

| 항목 | 관례 |
|---|---|
| 메서드 이름 | 영어 camelCase. 예: `verifyShouldFailWhenCountsDoNotMatch` |
| `@DisplayName` | **한국어 문장.** 행위와 결과를 함께 쓴다 |
| 단정 | AssertJ (`assertThat`, `assertThatThrownBy`) |
| 구조 | `// given` `// when` `// then` |
| 자동 설정 검증 | `ApplicationContextRunner` |
| 통합 테스트 | H2 + `@SpringBootTest(classes = AutoConfig.class, properties = "query-counter.enabled=true")` |

## 빌드

로컬 빌드는 **JDK 17**이다. `.jitpack.yml`이 `openjdk17`을 지정하고 있고
`java.toolchain`도 17이다.

```sh
./gradlew build
./gradlew test --tests '*AutoConfigTest*'
```

checkstyle, format, spotless 태스크는 없다.

## 릴리스

JitPack은 **배포하는 것이 아니라 요청받을 때 태그를 클론해 빌드한다.**
따라서 git 태그와 GitHub 릴리스를 만들면 릴리스가 끝난 것이고 따로 올릴 것이 없다.

- 설치 좌표는 `com.github.jjh75607:query-counter:<태그>`
- 릴리스 제목은 태그와 정확히 같게 쓴다 (`v0.0.6`. `v.0.0.6` 처럼 점이 끼지 않게)
- `CHANGELOG.md`는 Keep a Changelog 형식이고 버전별로 기록한다
- 릴리스 후 `https://jitpack.io/api/builds/com.github.jjh75607/query-counter` 로
  빌드 상태를 확인한다

## 하지 않을 것

- **테스트 라이브러리에 네트워크 호출을 넣지 않는다.** 대시보드나 리포트가 필요하면
  `build/reports/` 에 자기완결 파일을 쓴다. JaCoCo와 Gradle 테스트 리포트가 그 방식이다
- 529줄짜리 코드에 일반적인 추상화 작업을 하지 않는다. 다음 기능이 이음새를
  알려줄 때 그 자리만 뽑는다
- 기능 추가와 리팩터링을 한 PR에 섞지 않는다
