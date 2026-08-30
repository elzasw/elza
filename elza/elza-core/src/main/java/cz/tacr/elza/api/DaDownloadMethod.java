package cz.tacr.elza.api;

/**
 * How AIP packages are downloaded from a digital archive (DA) repository.
 */
public enum DaDownloadMethod {

    /**
     * Standard HTTP GET as defined by the DA OpenAPI specification.
     */
    STANDARD,

    /**
     * File Transfer protocol (com.lightcomp.ft), suitable for large packages.
     */
    FILE_TRANSFER,

}
