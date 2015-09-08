package cz.tacr.elza.ui.utils;

import java.util.function.Consumer;

import cz.req.ax.AxAction.ActionException;
import cz.tacr.elza.api.exception.ConcurrentUpdateException;

/**
 *
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 8. 2015
 */
public class ConcurrentUpdateExceptionHandler implements Consumer<ActionException> {

    @Override
    public void accept(ActionException ex) {
        ex.printStackTrace();
        if (ex.getCause() != null && ex.getCause() instanceof ConcurrentUpdateException) {
            ElzaNotifications.showError("Entita byla změněna nebo odstraněna. Načtěte znovu entitu a opakujte akci.");
        } else {
            throw ex;
        }
    }
}
