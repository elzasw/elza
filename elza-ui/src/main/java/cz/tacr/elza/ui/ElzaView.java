package cz.tacr.elza.ui;

import javax.annotation.PostConstruct;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.CssLayout;

import cz.req.ax.AxView;
import cz.tacr.elza.ui.view.FindingAidView;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public abstract class ElzaView extends AxView {

    @PostConstruct
    public void init() {
        actions(action(FindingAidView.class).caption("ELZA").icon(new ThemeResource("img/elza-logo.png")));
    }

    public CssLayout headerMain() {
        return rootLayout(pageHeader(), "header-main", false);
    }

    public CssLayout header() {
        return rootLayout(headerMain(), "header", false);
    }

    public CssLayout actionsBar() {
        return rootLayout(header(), "action-bar", false);
    }

    public CssLayout bodyHeadMain() {
        return bodyLayout("title-main");
    }

    public CssLayout bodyMain() {
        return bodyLayout("content-main");
    }

}