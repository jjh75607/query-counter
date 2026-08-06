package soon.springtestutil.config;

/**
 * 이 라이브러리가 이미 감싼 DataSource라는 표시입니다.
 *
 * <p>{@link DataSourceProxyBeanPostProcessor}가 DataSource를 감쌀 때 이 인터페이스를
 * 프록시에 실어 둡니다. 그러면 나중에 그 DataSource를 다시 만나도 감싸진 것임을 알 수
 * 있습니다.
 *
 * <p>표시가 필요한 이유는 감싼 결과물이 원본 클래스의 CGLIB 프록시이기 때문입니다.
 * 타입만 봐서는 감싸기 전과 구별되지 않습니다.
 *
 * <p>구현할 일이 없는 표시용 인터페이스입니다. 이 라이브러리 밖에서 쓰지 않습니다.
 */
public interface QueryCountedDataSource {
}
