package cz.tacr.elza.ui.components;

import javax.annotation.Nullable;

import com.vaadin.ui.Component;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.9.2015
 */
public interface Components extends cz.req.ax.Components {

    default CssLayout cssLayoutExt(@Nullable String name, Component... components){
        CssLayout layout = new CssLayout(components);
        if(name != null){
            layout.setStyleName(name);
        }
        layout.setSizeUndefined();

        return layout;
    }
}
