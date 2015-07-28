package cz.tacr.elza.ax;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxAction;
import cz.tacr.elza.ax.AxUtils;
import ru.xpoft.vaadin.VaadinView;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 16.1.15
 */

@Component
@Scope("prototype")
@VaadinView("Exception")
public class ExceptionView2 extends cz.tacr.elza.ax.AxView {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${vax.viewMain}")
    String viewMain;

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        AxUtils.closeWindows();
        AbstractComponent component;
        Throwable exception = getSession().getAttribute(Throwable.class);
        if (exception != null) {
            logException(exception);
            Accordion accordion = new Accordion();
            do {
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                TextArea stacktrace = new TextArea("Stacktrace", stringWriter.toString());
                stacktrace.setWidth(100, Unit.PERCENTAGE);
                stacktrace.setHeight(200, Unit.PIXELS);
                stacktrace.setReadOnly(true);

                accordion.addTab(stacktrace, exception.getMessage(), FontAwesome.EXCLAMATION_CIRCLE);
                component = stacktrace;

                if (exception.getCause() != null && !exception.getCause().equals(exception)) {
                    exception = exception.getCause();
                } else {
                    exception = null;
                }
            } while (exception != null);

            accordion.setSelectedTab(component);
            component = accordion;

        } else {
            component = new Label("Neznámá chyba :(");
        }

        Label label = new Label("Ale toto je nepříjemné ...");
        label.addStyleName("h2");
        label.addStyleName("primary");
        Button button = new Button("Pokračovat", e -> navigate(viewMain));
        components(label, button, component);
    }

    /**
     * Empiricky očistí výjimku a zaloguje do standardního logu.
     *
     * @param exception výjimka
     */
    private void logException(Throwable exception) {
        if (exception instanceof AxAction.ActionException) {
            exception = exception.getCause();
        }
        logger.error(exception.getMessage(), exception);
    }
}
