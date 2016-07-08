package cz.tacr.elza.service.websocket;

/**
 * Typ obálky do websoketu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 14.01.2016
 */
public enum WebsocketDataType {
    /**
     * Typ událost.
     */
    EVENT,
    /**
     * Typ validace.
     */
    VALIDATION;

}
