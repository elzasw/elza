package cz.tacr.elza.service.eventnotification.events;

/**
 * Událost informující o doběhnutí akce na serveru.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 25. 1. 2016
 */
public class ActionEvent extends AbstractEventSimple {

    public ActionEvent(EventType eventType) {
        super(eventType);
    }
}
