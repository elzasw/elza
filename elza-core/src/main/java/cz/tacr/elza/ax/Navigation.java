package cz.tacr.elza.ax;

import com.vaadin.ui.UI;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxView;
import ru.xpoft.vaadin.VaadinView;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 13.1.15
 */
@FunctionalInterface
public interface Navigation {

    String string = null;

    String getId();

    default cz.tacr.elza.ax.AxAction<String> action(Class<? extends cz.tacr.elza.ax.AxView> viewClass) {
        String viewName = viewName(viewClass);
        return new cz.tacr.elza.ax.AxAction<String>().caption(viewName).value(viewName).action(this::navigate);
    }

    default cz.tacr.elza.ax.AxAction<String> action(Class<? extends cz.tacr.elza.ax.AxView> viewClass, String userRole) {
        RequireRole annotation = viewClass.getAnnotation(RequireRole.class);
        cz.tacr.elza.ax.AxAction<String> action = action(viewClass);
        if (annotation != null && annotation.value() != null && annotation.value().length > 0) {
            action.disabled();
            if (Arrays.asList(annotation.value()).contains(userRole)) {
                action.enabled();
            }
        }
        return action;
    }

    default void navigate(Class<? extends cz.tacr.elza.ax.AxView> viewClass) {
        navigate(viewName(viewClass), null);
    }

    default void navigate(Class<? extends cz.tacr.elza.ax.AxView> viewClass, Object parameter) {
        navigate(viewName(viewClass), parameter);
    }

    default void navigate(String viewName) {
        navigate(viewName, null);
    }

    default void navigate(String viewName, Object parameter) {
        StringBuffer viewState = new StringBuffer(viewName);
        if (parameter != null) {
            viewState.append("/").append(parameter.toString());
        }
        UI.getCurrent().getNavigator().navigateTo(viewState.toString());
    }

    default String viewName(Class<? extends AxView> viewClass) {
        Annotation annotation = viewClass.getAnnotation(VaadinView.class);
        if (annotation == null)
            throw new RuntimeException("View class " + viewClass + " missing annotation VaadinView");
        return ((VaadinView) annotation).value();

    }

}
