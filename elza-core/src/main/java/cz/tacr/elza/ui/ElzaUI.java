package cz.tacr.elza.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.JavaScript;
import cz.req.ax.AxUI;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Component
@Scope("prototype")
@Theme("elza")
public class ElzaUI extends AxUI {

    @Override
    protected void init(VaadinRequest request) {
        getPage().setTitle("ELZA");
        addStyleName("elza");
        super.init(request);
    }

    @Override
    protected void navigate() {
//        if (tryNavigateProperty("vax.firstView")) return;
        super.navigate();
    }

    @Override
    public void afterViewChange(ViewChangeEvent event) {
        super.afterViewChange(event);
        JavaScript.getCurrent().execute("window.scrollTo(0,0)");
    }
}
