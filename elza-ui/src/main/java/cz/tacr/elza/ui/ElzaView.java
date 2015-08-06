package cz.tacr.elza.ui;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.CssLayout;
import cz.req.ax.AxView;
import cz.tacr.elza.ui.view.FindingAidListView;
import cz.tacr.elza.ui.view.FindingAidView;

import javax.annotation.PostConstruct;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public abstract class ElzaView extends AxView {

    public ElzaView() {
        setWrap(true);
    }

    @PostConstruct
    public void init() {
        actions(action(FindingAidListView.class).caption("ELZA").icon(new ThemeResource("img/elza-logo.png")));
    }

    public ElzaView pageTitle(String title) {
        pageTitle().addComponent(newLabel(title, "h1"));
        return this;
    }

    public CssLayout pageTitle() {
        return rootLayout(this, "page-title");
    }

}