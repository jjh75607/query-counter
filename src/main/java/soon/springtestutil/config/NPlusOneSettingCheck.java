package soon.springtestutil.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 검사를 켰지만 카운팅을 켜지 않은 구성을 알려 줍니다.
 *
 * <p>{@code query-counter.n-plus-one.enabled=true} 만 켜면 아무 일도 일어나지 않습니다.
 * {@link AutoConfig}가 {@code query-counter.enabled=true}에만 걸려 있어 DataSource를 감싸지
 * 않고, 감싸지 않으면 기록도 모드 전달도 없습니다. <b>실패하지 않고 조용히 안 도는 종류라</b>
 * 사용자는 검사가 도는 줄 압니다.
 *
 * <p>검사를 켠 구성에서만 로딩됩니다. 켜지 않은 프로젝트에는 이 클래스가 만들어지지도
 * 않으므로 아무 영향이 없습니다.
 *
 * <p>실패시키지 않고 경고만 남깁니다. 설정 실수로 남의 빌드를 세우지 않습니다.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "query-counter.n-plus-one", name = "enabled", havingValue = "true")
public class NPlusOneSettingCheck implements InitializingBean {

    private final Environment environment;

    public NPlusOneSettingCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        boolean countingEnabled = environment
            .getProperty("query-counter.enabled", Boolean.class, false);
        if (!countingEnabled) {
            log.warn("query-counter.n-plus-one.enabled is on but query-counter.enabled is not. "
                + "No query is recorded without it, so the N+1 check does nothing. "
                + "Set query-counter.enabled=true as well.");
        }
    }

}
