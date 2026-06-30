package cz.tacr.elza.domain;

/**
 * Persisted synchronisation state of {@link ApBindingState#syncOk}.
 *
 * Only the two physical states that can actually be stored are listed here.
 * The REST/UI tier exposes an additional {@code LOCAL_CHANGE} value via the
 * OpenAPI-generated {@code cz.tacr.elza.controller.vo.SyncState} — that one
 * is a derived state computed at read time (see {@code ExtEntityBindingFactory})
 * and is never written to the database, so it must not appear on this enum.
 */
public enum SyncState {

    SYNC_OK,

    NOT_SYNCED

}
