package cz.tacr.elza.ax;

import cz.tacr.elza.ax.*;


public class AxConfirm extends AxWindow {

    public AxConfirm(String message, Runnable confirm) {
        this(message, "Potvrdit", "Zrušit", confirm);
    }

    public AxConfirm(String message, String confirmCaption, String cancelCaption, Runnable confirm) {
        setSizeUndefined();
        modal().style("window-confirm").components(newLabel(message, "h2"));
        buttonPrimary(new cz.tacr.elza.ax.AxAction().caption(confirmCaption).run(confirm));
        buttonClose(cancelCaption);
    }

}
