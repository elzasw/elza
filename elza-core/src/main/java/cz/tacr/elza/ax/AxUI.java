package cz.tacr.elza.ax;

import com.google.common.eventbus.EventBus;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.ErrorHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import ru.xpoft.vaadin.DiscoveryNavigator;

import java.lang.reflect.Field;

public abstract class AxUI extends UI implements ViewChangeListener {

    @Autowired
    EventBus eventBus;
    @Autowired
    Environment environment;

    public AxUI() {
        setSizeUndefined();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public Environment getEnvironment() {
        return environment;
    }

    private ErrorHandler errorHandler = new DefaultErrorHandler() {
        @Override
        public void error(com.vaadin.server.ErrorEvent event) {
            super.error(event);
            UI.getCurrent().getSession().setAttribute(Throwable.class, event.getThrowable());
            UI.getCurrent().getNavigator().navigateTo(getEnvironment().getProperty("vax.viewError"));
        }
    };

    @Override
    protected void init(VaadinRequest request) {
        setErrorHandler(errorHandler);
        DiscoveryNavigator navigator = new DiscoveryNavigator(this, this);
        navigator.addViewChangeListener(this);
        setNavigator(navigator);
        try {
            if (StringUtils.isEmpty(navigator.getState())) {
                navigate();
            }
        } catch (Throwable th) {
            navigate(th);
        }
    }

    protected void navigate() {
        if (!tryNavigateProperty("vax.viewMain"))
            throw new IllegalArgumentException("Missing default view configuration property vax.viewMain");
    }

    protected void navigate(Throwable th) {
        getSession().setAttribute(Throwable.class, th);
        if (!tryNavigateProperty("vax.viewError"))
            throw new IllegalArgumentException("Missing default view configuration property vax.viewError");
    }

    protected boolean tryNavigateProperty(String propertyName) {
        String viewName = getEnvironment().getProperty(propertyName);
        if (StringUtils.isEmpty(viewName)) return false;
        getNavigator().navigateTo(viewName);
        return true;
    }

    @Override
    public void attach() {
        eventBus.register(this);
        super.attach();
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        try {
            Field field = Navigator.class.getDeclaredField("currentView");
            field.setAccessible(true);
            Object view = field.get(getNavigator());
            eventBus.unregister(view);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error unregister view " + e.getMessage());
        }
        super.detach();
    }

    @Override
    public boolean beforeViewChange(ViewChangeEvent event) {
        return true;
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
        // Nastává: java.lang.IllegalArgumentException: missing event subscriber for an annotated method.
        if (event.getOldView() != null) {
            try {
                eventBus.unregister(event.getOldView());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (event.getNewView() != null) {
            try {
                eventBus.register(event.getNewView());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
