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
  assertion/ExpectedCount           기대값과 비교 방식(정확히 N, N 이하)을 담는 값 객체
  assertion/QueryCountVerifier      실제 비교와 오류 메시지 조립
  extension/QueryCountTestExecutionListener  Spring 테스트용. spring.factories 로 자동 등록
  extension/QueryCountTestExtension          JUnit 확장. Spring 컨텍스트를 띄우지 않는 테스트용
```

흐름은 세 층이다. **수집(Listener, Context) → 명세(Assertion) → 검증(Verifier).**

## 주의할 지점

### BeanPostProcessor에 프로퍼티 빈을 주입하지 않는다

`BeanPostProcessor`는 일반 빈보다 먼저 만들어진다. `QueryCounterProperties`를
주입받으면 프로퍼티 빈이 너무 이르게 초기화되고 Spring이 경고를 낸다.
`AutoConfig`에서 `Environment`로 값을 읽어 생성자에 넘긴다.

### 애플리케이션 컨텍스트를 참조하지 않는다

`TestContext.getApplicationContext()`를 부르면 컨텍스트 로딩이 강제된다. 모든 Spring
테스트에서 도는 리스너에서는 받아들일 수 없다. ThreadLocal 정리는 활성 여부와 무관하게
무해하므로 조건 없이 수행한다.

활성 여부를 알려야 할 곳이 하나 있는데, 검증이 실패했고 기록된 쿼리가 0건일 때
`QueryCountVerifier`가 안내를 덧붙이는 자리다. 판단에 필요한 정보가 이미 ThreadLocal에
있으므로 별도 상태를 두지 않는다. 예전에 static 플래그를 썼다가 리셋되지 않아 테스트
불가능해져서 걷어냈다.

### 알려진 빚

건드릴 일이 있으면 함께 정리한다. 지금 당장 급하지는 않다.

| 위치 | 내용 |
|---|---|
| `QueryCountListener` | `queryTypeCache` 가 SQL 문자열 키의 static 맵인데 비워지지 않는다 |
| `QueryCountListener` | `elapsedMs` 가 실행 단위 값인데 배치의 모든 쿼리에 같은 값이 붙는다 |
| `QueryInfo` | 생성자가 테이블 이름을 항상 정규식으로 추출한다. 안 쓰는 경우에도 |
| `QueryCountVerifier` | 229줄에 private 메서드 13개. 검사를 하나 더 추가하기 전에 검사 단위를 인터페이스로 뽑는 편이 낫다 |
| `QueryCounterAssertion` | 검증하지 않은 어서션을 static ThreadLocal 목록으로 들고 있다. 리스너가 비우지만 전역 상태가 하나 늘어난 것은 사실이다 |

## 작업 규칙

**브랜치와 PR을 거친다. `master`에 직접 커밋하지 않는다.**

이슈는 항상 만들지 않는다. 아래 셋 중 하나라도 해당할 때만 만든다.

| 조건 | 왜 |
|---|---|
| 코드를 쓰기 전에 정해야 할 판단이 있다 | 결정과 그 근거가 diff 에 남지 않는다. 나중에 왜 그렇게 했는지 되짚을 곳이 필요하다 |
| 하나의 발견이 여러 PR로 갈린다 | PR들을 묶는 자리가 있어야 전체 그림이 보인다 |
| 남이 재현하거나 이어받을 정보가 diff 밖에 있다 | 재현 로그, 실측값, 외부 저장소 이슈 링크 같은 것들이다 |

해당하지 않으면 **PR 하나로 끝낸다.** 기계적인 수정, 문서 오타, 설정 추가가 여기 해당한다.
PR 본문에 배경과 판단을 적으면 이슈에 같은 내용을 한 번 더 쓸 이유가 없다.

판단이 서지 않으면 만들지 않는 쪽을 고른다. 필요해지면 그때 만들어 PR을 연결하면 된다.

| 항목 | 규칙 |
|---|---|
| base 브랜치 | `master` (단일 트렁크) |
| 유지보수 브랜치 | 지금은 없다. 이미 나간 버전을 패치해야 할 때 `0.0.x` 형태로 만든다 |
| 브랜치 이름 | 이슈가 있으면 `<type>/#<이슈번호>` 예: `feat/#42`. 없으면 `<type>/<짧은-설명>` 예: `chore/issue-policy` |
| 커밋과 PR 제목 | 이슈가 있으면 `[#이슈번호] Type: 한국어 설명`. 없으면 `Type: 한국어 설명` |
| Type | `Feat`, `Fix`, `Docs`, `Refactor`, `Chore`, `Setting`, `Test` |
| 머지 | 스쿼시. 제목에 ` (#PR번호)`가 붙는다 |
| 이슈 본문 | 이슈를 만들 때는 `.github/ISSUE_TEMPLATE/` 의 타입별 템플릿 |
| 라벨 | 타입 라벨(`Feat`, `Fix` 등)은 두지 않는다. 제목 접두사와 같은 말이라 정보가 없었다. GitHub 기본 라벨만 남겼고 템플릿이 자동으로 붙이지 않는다. 붙일지는 그때 판단한다 |
| PR 본문 | `.github/PULL_REQUEST_TEMPLATE.md` |

## 문서

| 파일 | 용도 |
|---|---|
| `README.md` | 영문. 기본 문서다. 사용자가 처음 보는 곳 |
| `README.ko.md` | 한국어. 두 문서 상단에서 서로 링크한다 |
| `CHANGELOG.md` | Keep a Changelog 형식. 릴리스 전 변경은 `[Unreleased]` 절에 쌓는다 |

**둘 중 하나만 고치지 않는다.** 사용법이나 API가 바뀌면 두 문서를 함께 고친다.

에러 메시지 형식을 README에 예시로 적어둔 곳이 있다. 메시지를 바꾸면 그 예시도 함께 고친다.
과거에 실행 시간 메시지가 코드와 어긋난 채 남아 있었다.

## 주석과 표기

### 주석 언어

읽는 사람이 누구인지로 갈린다. 공개 API 는 이 라이브러리를 쓰는 사람이 IDE 에서 보고,
내부 구현은 이 저장소를 고치는 사람만 본다.

| 대상 | 언어 |
|---|---|
| 공개 API 의 Javadoc | **영어.** 사용자가 IDE 자동완성과 Javadoc 에서 본다 |
| `QueryCounterProperties` 의 프로퍼티 설명 | **영어.** IDE 자동완성 메타데이터로 나간다 |
| 내부 클래스의 Javadoc | **한국어.** `AutoConfig`, `DataSourceProxyBeanPostProcessor`, `QueryCountVerifier` |
| 메서드 안의 구현 주석 (`//`) | **한국어.** 왜 이렇게 했는지를 적는 자리다 |
| 테스트 | **한국어.** `@DisplayName` 이 한국어라 영어 주석과 섞으면 읽기 나쁘다 |

공개 API 는 `QueryCounterAssertion`, `TableQueryAssertion`, `ExpectedCount`,
`QueryCountContext`, `QueryCountTestExtension`, `QueryCountTestExecutionListener`,
`QueryCounterProperties` 다.

### 다른 저장소의 이슈 번호

**백틱으로 감싼다.** `` `quick-perf/quickperf#199` `` 처럼 쓴다.

감싸지 않으면 GitHub 이 그 저장소에 상호 참조를 남긴다. 이미 닫힌 이슈에도 알림이 가고
타임라인이 지저분해진다. 남의 저장소에 노이즈를 남기지 않는다. 상호 참조는 한 번 생기면
본문을 고쳐도 지워지지 않으므로, 애초에 만들지 않는 것이 유일한 대응이다.

적용 범위는 GitHub 이 렌더링하는 곳 전부다. **커밋 메시지도 포함된다.** 놓치기 쉬운 자리다.

| 자리 | 적용 |
|---|---|
| 이슈와 PR 의 본문, 댓글 | 적용 |
| 커밋 메시지 | 적용. GitHub 이 링크로 만든다 |
| Java 소스의 Javadoc 과 주석 | 해당 없음. 렌더링되지 않는다. 백틱 대신 그대로 쓴다 |

링크가 꼭 필요하면 URL 도 백틱 안에 넣는다. 이 저장소 안의 이슈와 PR 번호는 그대로 쓴다.
상호 참조가 목적에 맞는다.

## 테스트 규칙

| 항목 | 관례 |
|---|---|
| 메서드 이름 | 영어 camelCase. 예: `verifyShouldFailWhenCountsDoNotMatch` |
| `@DisplayName` | **한국어 문장.** 행위와 결과를 함께 쓴다 |
| 주석 | 한국어 |
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
./gradlew spotlessApply
```

포맷 검사는 Spotless 다. `spotlessCheck` 가 `check` 에 물려 있어 `./gradlew build` 와
CI 가 함께 검사한다. 어긋나면 `./gradlew spotlessApply` 로 고친다.

**지금 스타일을 고정하는 설정이지 재포맷 도구가 아니다.** 규칙은 사용하지 않는 import 제거,
뒤 공백 제거, 파일 끝 개행, 들여쓰기 4칸 스페이스 네 가지뿐이다. spring-javaformat 은
들여쓰기가 탭이라 저장소 전체가 바뀌므로 쓰지 않는다. import 순서 규칙도 걸지 않았다.
checkstyle 은 없다.

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
- 코드가 작다. 일반적인 추상화 작업을 미리 하지 않는다. 다음 기능이 이음새를
  알려줄 때 그 자리만 뽑는다
- 기능 추가와 리팩터링을 한 PR에 섞지 않는다
