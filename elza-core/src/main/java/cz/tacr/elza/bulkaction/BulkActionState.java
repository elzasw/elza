package cz.tacr.elza.bulkaction;


import cz.tacr.elza.domain.ArrChange;


/**
 * Implementace stavu instance hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public class BulkActionState implements cz.tacr.elza.api.vo.BulkActionState<ArrChange> {

    /**
     * Změna
     */
    private ArrChange runChange;

    /**
     * Stav
     */
    private State state = State.WAITING;

    /**
     * Identifikace instance hromadné akce
     */
    private Integer processId = 0;

    /**
     * Kód hromadné akce
     */
    private String bulkActionCode;

    @Override
    public synchronized ArrChange getRunChange() {
        return runChange;
    }

    @Override
    public synchronized void setRunChange(final ArrChange runChange) {
        this.runChange = runChange;
    }

    @Override
    public synchronized State getState() {
        return state;
    }

    @Override
    public synchronized void setState(final State state) {
        this.state = state;
    }

    @Override
    public synchronized Integer getProcessId() {
        return processId;
    }

    @Override
    public synchronized void setProcessId(final Integer processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return "BulkActionState{" +
                "runChange=" + runChange +
                ", state=" + state +
                ", processId=" + processId +
                '}';
    }

    @Override
    public String getBulkActionCode() {
        return bulkActionCode;
    }

    @Override
    public void setBulkActionCode(final String bulkActionCode) {
        this.bulkActionCode = bulkActionCode;
    }
}
