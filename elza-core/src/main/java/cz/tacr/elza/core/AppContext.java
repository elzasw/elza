package cz.tacr.elza.core;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
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

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        AppContext.applicationContext = applicationContext;
    }
}
