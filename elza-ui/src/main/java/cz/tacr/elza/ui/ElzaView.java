package cz.tacr.elza.ui;

import javax.annotation.PostConstruct;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import cz.req.ax.AxView;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
public abstract class ElzaView extends AxView {

    @PostConstruct
    public void init() {
        Label elza = new Label("ELZA");
        elza.addStyleName("fa-header-elza");
        header().addComponent(elza);
    }

    public CssLayout headerMain() {
        return rootLayout(pageHeader(), "fa-header-main", false);
    }

    public CssLayout header() {
        return rootLayout(headerMain(), "fa-header", false);
    }

    public CssLayout actionsBar() {
        return rootLayout(header(), "fa-action-bar", false);
    }

    public CssLayout bodyHeadMain() {
        return bodyLayout("fa-title-main");
    }

    public CssLayout bodyMain() {
        return bodyLayout("fa-content-main");
    }

}