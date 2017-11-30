package cz.tacr.elza.dataexchange.input.context;

public interface ObservableImport {

    void registerPhaseChangeListener(ImportPhaseChangeListener phaseChangeListener);
}
