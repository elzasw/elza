package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;

import java.util.Date;
import java.util.List;

/**
 * Stav hromadných akcí
 *
 * @author Petr Compel [petr.compel@marbes.cz]
 * @since 29. 1. 2016
 */
public class BulkActionRunVO {

    private Integer id;

    private String code;

    private State state;

    private List<TreeNodeClient> nodes;

    private Date datePlanned;

    private Date dateStarted;

    private Date dateFinished;

    private String error;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<TreeNodeClient> getNodes() {
        return nodes;
    }

    public void setNodes(List<TreeNodeClient> nodes) {
        this.nodes = nodes;
    }

    public Date getDatePlanned() {
        return datePlanned;
    }

    public void setDatePlanned(Date datePlanned) {
        this.datePlanned = datePlanned;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDateFinished() {
        return dateFinished;
    }

    public void setDateFinished(Date dateFinished) {
        this.dateFinished = dateFinished;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
