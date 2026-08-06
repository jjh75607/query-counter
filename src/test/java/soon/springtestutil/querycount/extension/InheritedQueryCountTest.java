package soon.springtestutil.querycount.extension;

import org.junit.jupiter.api.DisplayName;

/**
 * 테스트 메서드를 하나도 선언하지 않고 상위 클래스에서 전부 물려받는다.
 * 상속 계층에서도 기록이 도는지 확인하는 것이 목적이다.
 */
@DisplayName("상속받은 테스트 클래스에서의 쿼리 기록")
class InheritedQueryCountTest extends AbstractInheritedQueryCountTest {
}
