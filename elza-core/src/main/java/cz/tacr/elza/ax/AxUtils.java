package cz.tacr.elza.ax;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.Iterator;

public class AxUtils {

    public static ErrorHandler EMPTY_ERROR_HANDLER = event -> {
        System.out.println(event);
    };

    /*public static <T> T getDataValue(Class<T> type, Property.ValueChangeEvent event) {

        }*/
    public static Integer getTabPosition(TabSheet tabSheet) {
        return tabSheet.getTabPosition(tabSheet.getTab(tabSheet.getSelectedTab()));
    }

    public static <T> T getDataValue(Button.ClickEvent event) {
        Object source = event.getSource();
        if (source instanceof AbstractComponent) {
            Object data = ((AbstractComponent) source).getData();
            return (T) data;
        }
        throw new IllegalArgumentException();
    }

    public static void closeWindows() {
        for (Window window : new ArrayList<>(UI.getCurrent().getWindows())) {
            UI.getCurrent().removeWindow(window);
        }
    }

    public static boolean focusFirst(Component component) {
        if (component == null) return false;
        if (component instanceof HasComponents) {
            Iterator<Component> iterator = ((HasComponents) component).iterator();
            while (iterator.hasNext()) {
                if (focusFirst(iterator.next())) {
                    return true;
                }
            }
        } else if (component instanceof AbstractTextField) {
            ((AbstractTextField) component).focus();
            return true;
        }
        return false;
    }

    public static Integer getParameterInteger(ViewChangeListener.ViewChangeEvent event) {
        try {
            return Integer.parseInt(event.getParameters());
        } catch (Exception ex) {
            return null;
        }
    }
}
