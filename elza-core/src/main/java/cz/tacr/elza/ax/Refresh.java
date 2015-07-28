package cz.tacr.elza.ax;

import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

@FunctionalInterface
public interface Refresh {

    void refresh();

    static void tryRefresh(Object o) {
        if (o instanceof Refresh) {
            ((Refresh) o).refresh();
        }
    }

    default void pushRefresh() {
        System.err.println("Refresh TicketChange");
        UI.getCurrent().access(new Runnable() {
            @Override
            public void run() {
                try {
//                    refresh();
                    Notification.show("Refresh");
//                UI.getCurrent().push();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
