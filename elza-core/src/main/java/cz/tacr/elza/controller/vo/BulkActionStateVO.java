package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.vo.BulkActionState;

/**
 * Stav hromadných akcí
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
public class BulkActionStateVO {

    private String code;

    private BulkActionState.State state;

    private int processId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BulkActionState.State getState() {
        return state;
    }

    public void setState(BulkActionState.State state) {
        this.state = state;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }
}
