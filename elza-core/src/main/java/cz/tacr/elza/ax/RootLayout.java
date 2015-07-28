package cz.tacr.elza.ax;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;

import java.util.function.Supplier;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxAction;
import cz.tacr.elza.ax.AxMenuBar;
import cz.tacr.elza.ax.InitMap;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 13.1.15
 */
public class RootLayout extends CssLayout implements cz.tacr.elza.ax.Components {

    public static final String PAGE_MENU = "page-menu";
    public static final String PAGE_HEAD = "page-header";
    public static final String PAGE_FOOT = "page-footer";
    public static final String PAGE_BODY = "page-body";
    public static final String MAIN_PANEL = "main-panel";

    private boolean wrap = false;
    private cz.tacr.elza.ax.InitMap layouts = new InitMap();

    public RootLayout() {
        setSizeUndefined();
    }

    public RootLayout mainComponents(Component... components) {
        mainPanel().addComponents(components);
        return this;
    }

    public RootLayout menuActions(AxAction... actions) {
        menuBar().actions(actions);
        return this;
    }

    public cz.tacr.elza.ax.AxMenuBar menuBar() {
        return getVariable(PAGE_MENU, () -> {
            cz.tacr.elza.ax.AxMenuBar menuBar = new AxMenuBar();
            menuBar.addStyleName("menu-bar");
            pageHeader().addComponent(menuBar);
            return menuBar;
        });
    }

    public CssLayout mainPanel() {
        return bodyLayout(MAIN_PANEL);
    }

    public CssLayout bodyLayout(String name) {
        return rootLayout(pageBody(), name, false);
    }

    public CssLayout pageHeader() {
        return rootLayout(this, PAGE_HEAD);
    }

    public CssLayout pageBody() {
        return rootLayout(this, PAGE_BODY);
    }

    public CssLayout pageFooter() {
        return rootLayout(this, PAGE_FOOT);
    }

    public CssLayout rootLayout(AbstractLayout container, String name) {
        return rootLayout(container, name, wrap);
    }

    public CssLayout rootLayout(AbstractLayout container, String name, boolean wrap) {
        return getVariable(name, () -> {
            try {
                CssLayout layout = cssLayout(name);
                container.addComponent(wrap ? cssLayout(name + "-wrap", layout) : layout);
                return layout;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    public <T> T getVariable(String key, Supplier<T> sup) {
        return layouts.get(key, sup);
    }
}
