package soon.springtestutil.config;

import jakarta.annotation.Nonnull;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils;
import soon.springtestutil.querycount.datasource.QueryCountListener;

import javax.sql.DataSource;
import java.lang.reflect.Method;

/**
 * DataSource 빈을 프록시로 감싸 실행된 쿼리를 기록하는 BeanPostProcessor입니다.
 *
 * <p>{@link AutoConfig}가 {@code query-counter.enabled=true}인 경우에만 이 빈을 등록합니다.
 * 따라서 활성화하지 않은 프로젝트에서는 DataSource가 감싸지지 않습니다.
 *
 * <p>SQL 로깅은 {@code query-counter.logging.enabled}로 따로 켭니다. 쿼리 카운팅과 무관한
 * 기능이고 테스트가 많은 프로젝트에서는 로그가 오염되므로 기본값은 비활성입니다.
 *
 * <p>{@link PriorityOrdered}를 구현합니다. Spring은 {@code PriorityOrdered} BeanPostProcessor를
 * 가장 먼저 만들므로, {@code Ordered}만 구현하면서 DataSource에 의존하는 다른
 * BeanPostProcessor가 있어도 그보다 먼저 등록됩니다. 이것이 없으면 그런 BeanPostProcessor를
 * 만드느라 DataSource가 미리 만들어져 감싸지지 않고, 기록된 쿼리가 0건이 됩니다.
 **/
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    private final boolean loggingEnabled;

    public DataSourceProxyBeanPostProcessor(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * {@code PriorityOrdered} 안에서는 가장 늦게 둡니다. DataSource를 감싸는 일은 다른
     * BeanPostProcessor가 DataSource에 손댈 기회를 가진 뒤에 하는 편이 안전합니다.
     */
    @Override
    public int getOrder() {
        return PriorityOrdered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    public Object postProcessAfterInitialization(
        @Nonnull Object bean,
        @Nonnull String beanName
    ) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
            ProxyFactory factory = new ProxyFactory(bean);
            factory.setProxyTargetClass(true); // 다양한 DataSource 구현체를 지원하기 위해 CGLIB 프록시 사용
            factory.addAdvice(ProxyDataSourceInterceptor.of((DataSource) bean, this.loggingEnabled));
            return factory.getProxy();
        }

        return bean;
    }

    /**
     * 실제 DataSource를 ProxyDataSource로 감싸고, 메서드 호출을 위임합니다.
     */
    private record ProxyDataSourceInterceptor(DataSource dataSource) implements MethodInterceptor {

        private static ProxyDataSourceInterceptor of(DataSource dataSource, boolean loggingEnabled) {
            ChainListener listener = new ChainListener();
            if (loggingEnabled) {
                listener.addListener(new SLF4JQueryLoggingListener());
            }
            listener.addListener(new QueryCountListener());

            return new ProxyDataSourceInterceptor(
                ProxyDataSourceBuilder.create(dataSource)
                    .name("DataSource-Proxy")
                    .listener(listener)
                    .build()
            );
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 실제 DataSource의 메소드를 프록시 DataSource에서 호출
            Method proxyMethod = ReflectionUtils.findMethod(
                dataSource.getClass(),
                invocation.getMethod().getName(),
                invocation.getMethod().getParameterTypes()
            );
            if (proxyMethod != null) {
                // 프록시 객체에 메서드가 존재하면 해당 메서드를 호출
                return proxyMethod.invoke(dataSource, invocation.getArguments());
            }
            return invocation.proceed();
        }

    }

}
