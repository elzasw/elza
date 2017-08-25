package cz.tacr.elza.deimport.context;

/**
 * Listener for import phase changes.
 */
public interface ImportPhaseChangeListener {

    /**
     * Handles import phase change.
     *
     * @return True when listener should be notified again.
     */
    boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context);
}
