package cz.tacr.elza.ax;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import cz.tacr.elza.ax.*;
import cz.tacr.elza.ax.AxAction;
import cz.tacr.elza.ax.AxUtils;


public class AxWindow extends RootLayout implements Navigation, Components {

    Window window;

    public AxWindow() {
        setStyleName("window-root");
        window = new Window();
        window.addStyleName("window-headerless");
        window.setSizeUndefined();
        window.setResizable(false);
        window.setClosable(false);
        window.setContent(this);
    }

    public Window getWindow() {
        return window;
    }

    @Override
    public cz.tacr.elza.ax.AxMenuBar menuBar() {
        return getVariable(PAGE_MENU, () -> {
            cz.tacr.elza.ax.AxMenuBar menuBar = new cz.tacr.elza.ax.AxMenuBar();
            menuBar.addStyleName("menu-bar");
            pageFooter().addComponent(menuBar);
            return menuBar;
        });
    }

    //TODO Burianek Review??
    public AxWindow show(UI ui) {
        ui.addWindow(window);
        if (!AxUtils.focusFirst(mainPanel())) {
            focus();
        }
        return this;
    }
    public AxWindow show() {
        window.center();
        show(UI.getCurrent());
        return this;
    }

    //TODO Burianek Review
    public AxWindow showTopRight() {
        window.setPositionX(UI.getCurrent().getPage().getBrowserWindowWidth() - Math.round(window.getWidth()) - 2);
        window.setPositionY(42);
        show(UI.getCurrent());
        return this;
    }

    public AxWindow show(int x, int y) {
        window.setPositionX(x);
        window.setPositionY(y);
        show(UI.getCurrent());
        return this;
    }

    public AxWindow close() {
        window.close();
        return this;
    }

    public AxWindow modal() {
        window.setModal(true);
        return this;
    }

    public AxWindow size(Integer width, Integer height) {
        if (width != null) window.setWidth(width, Unit.PIXELS);
        if (height != null) window.setHeight(height, Unit.PIXELS);
        return this;
    }

    public AxWindow style(String styleName) {
        window.addStyleName(styleName);
        return this;
    }

    public AxWindow caption(String caption) {
        pageHeader().addComponent(newLabel(caption, "window-caption"));
        return this;
    }

    public AxWindow components(Component... components) {
        mainComponents(components);
        return this;
    }

    public AxWindow errorHandler(ErrorHandler errorHandler) {
        setErrorHandler(errorHandler);
        return this;
    }

    public AxWindow buttonClose() {
        return buttonClose("Zavřít");
    }

    public AxWindow buttonClose(String caption) {
        cz.tacr.elza.ax.AxAction action = new cz.tacr.elza.ax.AxAction().caption(caption).run(window::close).right();
        addShortcutListener(action.shortcutListener(ShortcutAction.KeyCode.ESCAPE));
        menuBar().actions(action);
        return this;
    }

    public AxWindow buttonAndClose(cz.tacr.elza.ax.AxAction action) {
        Runnable runAfter = action.getRunAfter();
        if (runAfter != null) {
            action.runAfter(() -> {
                runAfter.run();
                window.close();
            });
        } else {
            action.runAfter(window::close);
        }
        menuBar().actions(action.right());
        return this;
    }

    public AxWindow buttonPrimary(AxAction action) {
        addShortcutListener(action.shortcutListenerEnter());
        buttonAndClose(action.primary());
        return this;
    }

    public AxWindow closeListener(Window.CloseListener listener) {
        window.addCloseListener(listener);
        return this;
    }

}
