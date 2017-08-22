package cz.tacr.elza.deimport.context;

public interface ImportObserver {

    void registerPhaseChangeListener(ImportPhaseChangeListener phaseChangeListener);
}
