package cz.tacr.elza.domain;

/**
 * Kind of target environment a publication type connects to.
 *
 * Persisted as a string in {@code arr_export_type.connection_type} and exposed
 * to the UI through the {@code PublicationType} VO. Currently descriptive only —
 * does not gate any backend behaviour.
 */
public enum ConnectionType {
    DEVELOPMENT,
    TEST,
    PRODUCTION
}
