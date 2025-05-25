package soon.springtestutil.config;

import jakarta.annotation.Nonnull;
import java.lang.reflect.Method;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * DataSource 빈을 프록시로 감싸는 BeanPostProcessor입니다.
 * 이 프로세서는 DataSource 빈이 초기화된 후에 호출되어, SLF4J를 사용하여 쿼리를 로깅하는 ProxyDataSource로 감쌉니다.
 **/
@Component
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(
        @Nonnull Object bean,
        @Nonnull String beanName
    ) throws BeansException {
        if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
            ProxyFactory factory = new ProxyFactory(bean);
            factory.setProxyTargetClass(true); // 다양한 DataSource 구현체를 지원하기 위해 CGLIB 프록시 사용
            factory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean));
            return factory.getProxy();
        }

        return bean;
    }

    /**
     * 실제 DataSource를 ProxyDataSource로 감싸고, 메서드 호출을 위임합니다.
     */
    private record ProxyDataSourceInterceptor(DataSource dataSource) implements MethodInterceptor {

        private ProxyDataSourceInterceptor(DataSource dataSource) {
            ChainListener listener = new ChainListener();
            listener.addListener(new SLF4JQueryLoggingListener());

            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                .name("DataSource-Proxy")
                .listener(listener)
                .build();
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