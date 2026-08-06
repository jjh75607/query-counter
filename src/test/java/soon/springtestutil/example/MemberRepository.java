package soon.springtestutil.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 팀을 지연 로딩으로 남겨 둡니다. 팀 이름을 꺼내면 회원 수만큼 추가 조회가 발생합니다.
     */
    @Query("select m from Member m")
    List<Member> findAllLazily();

    /**
     * 팀을 함께 읽습니다. 조회가 한 번으로 끝납니다.
     */
    @Query("select m from Member m join fetch m.team")
    List<Member> findAllWithTeam();

}
