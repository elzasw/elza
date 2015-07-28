package cz.tacr.elza.ax;

import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

@FunctionalInterface
public interface Components {

    String getId();

    default CssLayout cssLayout(String name, Component... components) {
        CssLayout layout = new CssLayout(components);
        layout.setSizeUndefined();
        layout.setStyleName(name);
        return layout;
    }

    default <T extends AbstractLayout> T newLayout(Class<T> type, String style, Component... components) {
        try {
            T layout = type.newInstance();
            layout.setSizeUndefined();
            layout.setStyleName(style);
            if (components != null) layout.addComponents(components);
            return layout;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default Component newImage(Resource resource) {
//        Image image = new Image("", resource);
//        return image;
        Label label = new Label();
        label.addStyleName("image");
        label.setSizeUndefined();
        label.setIcon(resource);
        return label;
    }

    default Label newLabel(String content) {
        return newLabel(content, null);
    }

    default Label newLabel(String content, String style) {
        Label label = new Label(content);
        label.setSizeUndefined();
        label.addStyleName(style);
        return label;
    }

    default Label newCaption(String content) {
        return newLabel(content, "v-caption");
    }

    default Component undefine(Component component) {
        component.setSizeUndefined();
        return component;
    }

}
