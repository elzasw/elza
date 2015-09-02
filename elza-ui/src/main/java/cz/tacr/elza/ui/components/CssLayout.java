package cz.tacr.elza.ui.components;

import com.vaadin.ui.Component;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.9.2015
 */
public class CssLayout extends com.vaadin.ui.CssLayout {

    public CssLayout() {
    }

    public CssLayout(final Component... children) {
        super(children);
    }

    public CssLayout setMarginTop(final boolean marginTop) {
        if (marginTop) {
            addStyleName("margin-top");
        } else {
            removeStyleName("margin-top");
        }

        return this;
    }
}
