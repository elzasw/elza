package cz.tacr.elza.dataexchange.input.context;

/**
 * Defines import phase. Ordinal value must be preserved.
 */
public enum ImportPhase {
    INIT, ACCESS_POINTS, PARTIES, INSTITUTIONS, RELATIONS, SECTIONS, FINISHED;

    /**
     * @return True when specified phase is subsequent.
     */
    public boolean isSubsequent(ImportPhase phase) {
        return ordinal() < phase.ordinal();
    }
}
