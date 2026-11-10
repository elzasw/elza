package cz.tacr.elza.api;

/**
 * How ELZA authenticates its calls to the REST API of a digital archive (DA) repository.
 * File Transfer is not affected; it uses HTTP Basic whenever a username is set.
 */
public enum DaApiAuthentication {

    /**
     * Requests carry no authentication.
     */
    NONE,

    /**
     * Every request is signed with the API key (NDA-HMAC-SHA256).
     */
    API_KEY,

    /**
     * HTTP Basic with the username and password of the repository.
     */
    BASIC,

}
