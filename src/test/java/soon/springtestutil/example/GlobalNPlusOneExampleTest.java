package soon.springtestutil.example;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import soon.springtestutil.querycount.context.QueryCountContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 모든 테스트에서 도는 N+1 검사 예제입니다.
 *
 * <p>README 의 예제가 실제로 컴파일되고 돌아가는지 여기서 검증합니다.
 *
 * <p>이 클래스에는 어서션이 하나도 없습니다. 그것이 이 기능의 요점입니다.
 * {@code query-counter.n-plus-one.enabled=true} 만 켜면 테스트에 아무것도 안 적고 검사가
 * 돕니다.
 *
 * <p>{@code fail} 은 켜지 않았으므로 N+1 을 찾아도 경고 로그만 남고 테스트는 통과합니다.
 * <b>이 테스트가 통과하는 것 자체가 그 성질의 검증입니다.</b> 경고 문구가 실제로 나가는지는
 * {@code NPlusOneWatchTest} 가 따로 검증합니다. 경고는 테스트 본문이 끝난 뒤 리스너가 남기므로
 * 본문 안에서는 확인할 수 없습니다.
 */
@DisplayName("모든 테스트에서 도는 N+1 검사 예제")
@SpringBootTest(classes = ExampleApplication.class, properties = {
    "query-counter.enabled=true",
    "query-counter.n-plus-one.enabled=true"
})
@Transactional
class GlobalNPlusOneExampleTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    EntityManager entityManager;

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

    @DisplayName("어서션을 하나도 안 적어도 N+1 이 경고로 잡히고 테스트는 통과한다")
    @Test
    void reportsNPlusOneWithoutAnyAssertion() {
        // given
        saveMembersInSeparateTeams(3);
        QueryCountContext.clear();

        // when - 회원을 읽고 각자의 팀 이름을 꺼낸다. 팀 조회가 회원 수만큼 붙는다
        memberRepository.findAllLazily().forEach(member -> member.getTeam().getName());

        // then - 적을 것이 없다. 검사는 이 메서드가 끝난 뒤에 돈다
    }

    @DisplayName("join fetch 로 N+1 을 없애면 경고도 나오지 않는다")
    @Test
    void reportsNothingAfterJoinFetch() {
        // given
        saveMembersInSeparateTeams(3);
        QueryCountContext.clear();

        // when
        memberRepository.findAllWithTeam().forEach(member -> member.getTeam().getName());

        // then - 적을 것이 없다
    }

}
