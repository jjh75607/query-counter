package soon.springtestutil.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * 테스트용 DataSource 를 직접 등록합니다.
 *
 * <p>Spring Boot 의 {@code DataSourceAutoConfiguration} 을 쓰지 않는 이유는 그 클래스가
 * Spring Boot 4 에서 패키지를 옮겼기 때문입니다. 여기서 확인하려는 것은 DataSource 가
 * 감싸지는지 여부이지 Boot 가 DataSource 를 어떻게 만드는지가 아니므로, 직접 만들어
 * 버전에 묶이지 않게 합니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
    }

}
