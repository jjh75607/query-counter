package consumer;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 스모크 테스트가 띄우는 최소 애플리케이션이다. 이 프로젝트에는 엔티티도 리포지토리도
 * 없고, H2 DataSource 하나만 자동 설정으로 올라온다.
 */
@SpringBootApplication
public class SmokeApp {

}
