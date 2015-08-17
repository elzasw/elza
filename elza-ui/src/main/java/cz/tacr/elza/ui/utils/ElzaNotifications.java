package cz.tacr.elza.ui.utils;

import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 17.8.2015
 */
public class ElzaNotifications {

    public static void show(final String text) {
        show(text, 2000);
    }

    public static void show(final String text, final int delay) {
        Notification notification = createNotification(text, delay);
        notification.show(UI.getCurrent().getPage());
    }

    public static void showWarn(final String text) {
        Notification notification = createNotification(text, 2000);
        notification.setStyleName("warning");
        notification.show(UI.getCurrent().getPage());
    }


    public static void showError(final String text) {
        Notification notification = createNotification(text, 2000);
        notification.setStyleName("error");
        notification.show(UI.getCurrent().getPage());
    }

    private static Notification createNotification(final String text, final int delay){
        Notification notification = new Notification(text);
        notification.setDelayMsec(delay);
        notification.setPosition(Position.BOTTOM_RIGHT);
        return notification;
    }

}
