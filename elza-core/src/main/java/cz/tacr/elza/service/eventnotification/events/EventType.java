package cz.tacr.elza.service.eventnotification.events;

/**
 * Typ události o změně, která bude odeslána klientovi.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public enum EventType {

    FINDING_AID_CREATE,
    FINDING_AID_DELETE,


    PARTY_CREATE,
    PARTY_UPDATE,

    RECORD_CREATE,
    RECORD_UPDATE;


}
