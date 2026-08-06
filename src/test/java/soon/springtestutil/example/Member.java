package soon.springtestutil.example;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * 예제용 엔티티입니다. 팀을 지연 로딩으로 참조하므로, 회원 목록을 읽고 각자의 팀 이름을
 * 꺼내면 회원 수만큼 추가 조회가 발생합니다. 그것이 N+1입니다.
 */
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;

    protected Member() {
    }

    public Member(String name, Team team) {
        this.name = name;
        this.team = team;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Team getTeam() {
        return team;
    }

}
