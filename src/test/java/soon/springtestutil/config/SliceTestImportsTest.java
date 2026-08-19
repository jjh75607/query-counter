package soon.springtestutil.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 슬라이스 테스트 등록 파일이 발행물에 실려 나가는지 봅니다.
 *
 * <p>{@code @DataJpaTest} 같은 슬라이스는 자동 설정을 목록으로 골라 켜므로, 이 파일이 없으면 그
 * 테스트에서 DataSource 가 감싸지지 않고 쿼리가 하나도 기록되지 않습니다. 실패하지 않고 조용히
 * 아무것도 안 하는 종류입니다.
 *
 * <p>파일 이름이 Spring Boot 버전마다 다릅니다. 3.x 는 슬라이스 애노테이션이
 * {@code test.autoconfigure} 아래에 모여 있고, 4.x 는 모듈별로 흩어져 있습니다. 모르는 쪽은
 * 무시되므로 슬라이스마다 두 이름을 함께 넣습니다.
 *
 * <p>덮는 슬라이스는 {@code @DataJpaTest}, {@code @JdbcTest}, {@code @DataJdbcTest} 셋입니다.
 * {@code @WebMvcTest} 는 DataSource 가 없어 대상이 아닙니다.
 *
 * <p>실제로 슬라이스 안에서 기록되는지는 이 테스트가 보지 못합니다. 애노테이션의 import 경로가
 * 버전마다 달라 한 소스로 두 버전을 컴파일할 수 없습니다. 그쪽은 실제 프로젝트에 붙여
 * 확인했습니다.
 */
@DisplayName("슬라이스 테스트 등록 파일")
class SliceTestImportsTest {

    private static final String PREFIX = "META-INF/spring/";

    /**
     * 슬라이스마다 3.x 이름과 4.x 이름이 한 쌍입니다.
     */
    private static final List<String> SLICE_IMPORTS = List.of(
        // @DataJpaTest
        "org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa.imports",
        "org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa.imports",
        // @JdbcTest
        "org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc.imports",
        "org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc.imports",
        // @DataJdbcTest
        "org.springframework.boot.test.autoconfigure.data.jdbc.AutoConfigureDataJdbc.imports",
        "org.springframework.boot.data.jdbc.test.autoconfigure.AutoConfigureDataJdbc.imports");

    static Stream<String> sliceImports() {
        return SLICE_IMPORTS.stream();
    }

    @DisplayName("등록 파일이 클래스패스에 있고 자동 설정을 가리킨다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("sliceImports")
    void importsFileShouldNameAutoConfig(String resource) throws IOException {
        // given, when
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PREFIX + resource)) {
            // then
            assertThat(in).as("등록 파일이 없다: %s", resource).isNotNull();
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(content.trim()).isEqualTo(AutoConfig.class.getName());
        }
    }

    @DisplayName("슬라이스 셋에 버전별로 두 개씩, 여섯 개를 덮는다")
    @Test
    void everySliceShouldHaveBothVersionNames() {
        // given, when, then - 새 슬라이스를 넣으면 두 이름이 한 쌍으로 늘어야 한다
        assertThat(SLICE_IMPORTS).hasSize(6).doesNotHaveDuplicates();

        // 3.x 는 슬라이스 애노테이션이 boot.test.autoconfigure 아래 모여 있고, 4.x 는 모듈별로
        // 흩어져 있다(boot.data.jpa.test.autoconfigure 처럼).
        assertThat(SLICE_IMPORTS.stream().filter(name -> name.contains(".boot.test.autoconfigure.")))
            .as("3.x 이름")
            .hasSize(3);
        assertThat(SLICE_IMPORTS.stream().filter(name -> !name.contains(".boot.test.autoconfigure.")))
            .as("4.x 이름")
            .hasSize(3);
    }

}
