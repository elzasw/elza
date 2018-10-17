package cz.tacr.elza.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Přístup k aplikačnímu kontextu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 18. 8. 2016
 */
@Component
public class AppContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static <T> T getBean(final Class<T> cls) {
        return applicationContext.getBean(cls);
    }

    /**
     * autowire bean
     *
     * @see AutowireCapableBeanFactory#autowireBean(java.lang.Object)
     */
    public static void autowireBean(Object bean) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
    }

    /**
     * add application context listener
     *
     * @see ConfigurableApplicationContext#addApplicationListener(org.springframework.context.ApplicationListener)
     */
    public static void addApplicationListener(ApplicationListener<?> listener) {
        ((ConfigurableApplicationContext) applicationContext).addApplicationListener(listener);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        AppContext.applicationContext = applicationContext;
    }
}
