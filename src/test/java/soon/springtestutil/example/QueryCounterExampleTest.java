package soon.springtestutil.example;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import soon.springtestutil.querycount.assertion.QueryCounterAssertion;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * query-counter 사용 예제입니다.
 *
 * <p>README 의 예제가 실제로 컴파일되고 돌아가는지 여기서 검증합니다. 문서만 있으면 API 가
 * 바뀔 때 조용히 낡습니다.
 *
 * <p>알아둘 점이 둘 있습니다.
 *
 * <ul>
 * <li>{@code @ExtendWith} 를 붙이지 않았습니다. Spring 테스트에서는 필요하지 않습니다.
 * <li><b>{@code @BeforeEach} 와 테스트 본문의 셋업 쿼리도 카운트에 포함됩니다.</b> 리스너가
 *     테스트 시작 직전에 초기화하기 때문입니다. 셋업 쿼리를 세고 싶지 않으면 셋업 뒤에
 *     {@link QueryCountContext#clear()} 를 부르면 됩니다.
 * </ul>
 */
@DisplayName("query-counter 사용 예제")
@SpringBootTest(classes = ExampleApplication.class, properties = "query-counter.enabled=true")
@Transactional
class QueryCounterExampleTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    EntityManager entityManager;

    /**
     * 회원마다 팀을 따로 만듭니다. 팀을 공유하면 영속성 컨텍스트가 팀을 한 번만 읽어
     * N+1 이 드러나지 않습니다.
     */
    private List<Member> saveMembersInSeparateTeams(int count) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Team team = teamRepository.save(new Team("팀" + i));
            members.add(memberRepository.save(new Member("회원" + i, team)));
        }
        entityManager.flush();
        entityManager.clear();
        return members;
    }

    @DisplayName("실행한 쿼리 종류별 횟수를 검증한다")
    @Test
    void countByQueryType() {
        // given - 팀 1건과 회원 1건을 저장하므로 INSERT 2회
        saveMembersInSeparateTeams(1);

        // when
        memberRepository.findAllLazily();

        // then
        QueryCounterAssertion.assertCounts()
            .insert(2)
            .select(1)
            .verify();
    }

    @DisplayName("셋업 쿼리를 세고 싶지 않으면 셋업 뒤에 기록을 비운다")
    @Test
    void clearAfterSetUpToIgnoreSetUpQueries() {
        // given
        saveMembersInSeparateTeams(3);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam();

        // then - 셋업의 INSERT 6회가 빠지고 조회만 남는다
        QueryCounterAssertion.assertCounts()
            .select(1)
            .insert(0)
            .verify();
    }

    @DisplayName("팀을 지연 로딩으로 두면 회원 수만큼 추가 조회가 발생한다")
    @Test
    void lazyLoadingCausesNPlusOne() {
        // given
        List<Member> members = saveMembersInSeparateTeams(3);
        QueryCountContext.clear();

        // when - 회원을 읽고 각자의 팀 이름을 꺼낸다
        memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

        // then - 회원 조회 1회에 팀 조회가 회원 수만큼 붙는다. 이것이 N+1 이다.
        // 기대값을 코드로 계산할 수 있어 데이터 개수가 바뀌어도 표현이 유지된다.
        QueryCounterAssertion.assertCounts()
            .select(1 + members.size())
            .verify();
    }

    @DisplayName("join fetch 로 팀을 함께 읽으면 회원이 몇 명이든 조회가 한 번이다")
    @Test
    void joinFetchAvoidsNPlusOne() {
        // given
        saveMembersInSeparateTeams(3);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam().forEach(member -> member.getTeam().getName());

        // then
        QueryCounterAssertion.assertCounts()
            .select(1)
            .verify();
    }

    @DisplayName("테이블마다 다른 조건을 검증한다")
    @Test
    void differentExpectationsPerTable() {
        // given
        saveMembersInSeparateTeams(2);
        QueryCountContext.clear();

        // when
        memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

        // then - 회원은 한 번에 읽고, 팀은 회원마다 한 번씩 읽는다
        QueryCounterAssertion.assertCounts()
            .forTable("member").select(1)
            .forTable("team").select(2)
            .verify();
    }

    @DisplayName("지정한 테이블의 쿼리만 검증하고 나머지는 무시한다")
    @Test
    void assertOnlyTheGivenTables() {
        // given
        saveMembersInSeparateTeams(2);

        // when
        memberRepository.findAllLazily();

        // then - team 테이블의 INSERT 2회는 검증 대상이 아니다
        QueryCounterAssertion.assertCounts()
            .forTables("member")
            .insert(2)
            .select(1)
            .verify();
    }

    @DisplayName("개별 쿼리의 실행 시간 상한을 쿼리 수와 함께 검증한다")
    @Test
    void assertExecutionTimeAlongWithCounts() {
        // given
        saveMembersInSeparateTeams(1);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam();

        // then - 상한을 넘는 쿼리가 하나라도 있으면 AssertionError 가 발생한다.
        // 여기서는 H2 인메모리라 모든 쿼리가 상한 안에 들어온다.
        QueryCounterAssertion.assertCounts()
            .select(1)
            .maxExecutionTimeMs(1000)
            .verify();
    }

    @DisplayName("기대한 쿼리 수와 실제가 다르면 어긋난 항목을 모아 보고한다")
    @Test
    void reportsEveryMismatchTogether() {
        // given
        saveMembersInSeparateTeams(1);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam();

        // then - SELECT 와 INSERT 둘 다 어긋나므로 두 줄이 함께 보고된다
        assertThatThrownBy(() -> QueryCounterAssertion.assertCounts()
            .select(5)
            .insert(3)
            .verify())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("QueryType.SELECT: expected 5, but was 1")
            .hasMessageContaining("QueryType.INSERT: expected 3, but was 0");
    }

    @DisplayName("케이스마다 기대값이 다른 파라미터화 테스트에서도 쓸 수 있다")
    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    void expectationPerParameterizedCase(int memberCount) {
        // given
        saveMembersInSeparateTeams(memberCount);
        QueryCountContext.clear();

        // when
        memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

        // then - 애노테이션으로는 표현할 수 없는 형태다
        QueryCounterAssertion.assertCounts()
            .select(1 + memberCount)
            .verify();
    }

    @DisplayName("verify() 를 생략해도 테스트가 끝날 때 자동으로 검증된다")
    @Test
    void verifyIsOptional() {
        // given
        saveMembersInSeparateTeams(1);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam();

        // then - verify() 를 부르지 않는다. 조건이 맞으면 통과하고,
        // 틀렸다면 이 테스트가 끝날 때 실패한다
        QueryCounterAssertion.assertCounts()
            .select(1);
    }

}
