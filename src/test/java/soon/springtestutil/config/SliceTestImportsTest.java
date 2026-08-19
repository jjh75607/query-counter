package soon.springtestutil.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 슬라이스 테스트 등록 파일이 발행물에 실려 나가는지 봅니다.
 *
 * <p>{@code @DataJpaTest} 는 자동 설정을 목록으로 골라 켜므로, 이 파일이 없으면 그 테스트에서
 * DataSource 가 감싸지지 않고 쿼리가 하나도 기록되지 않습니다. 실패하지 않고 조용히 아무것도
 * 안 하는 종류입니다.
 *
 * <p>파일 이름이 Spring Boot 버전마다 다릅니다. 3.x 는 {@code test.autoconfigure.orm.jpa},
 * 4.x 는 {@code data.jpa.test.autoconfigure} 아래에 애노테이션이 있습니다. 모르는 쪽은 무시되므로
 * 둘 다 넣습니다.
 *
 * <p>실제로 {@code @DataJpaTest} 안에서 기록되는지는 이 테스트가 보지 못합니다. 애노테이션의
 * import 경로가 버전마다 달라 한 소스로 두 버전을 컴파일할 수 없습니다. 그쪽은 실제 프로젝트에
 * 붙여 확인했습니다.
 */
@DisplayName("슬라이스 테스트 등록 파일")
class SliceTestImportsTest {

    @DisplayName("등록 파일이 클래스패스에 있고 자동 설정을 가리킨다")
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "META-INF/spring/org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa.imports",
        "META-INF/spring/org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa.imports"
    })
    void importsFileShouldNameAutoConfig(String resource) throws IOException {
        // given, when
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            // then
            assertThat(in).as("등록 파일이 없다: %s", resource).isNotNull();
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content.trim()).isEqualTo(AutoConfig.class.getName());
        }
    }

}
