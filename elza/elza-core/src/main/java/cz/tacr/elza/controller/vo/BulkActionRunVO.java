package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;

import cz.tacr.elza.domain.ArrBulkActionRun.State;

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

    private List<TreeNodeVO> nodes;

    private Date datePlanned;

    private Date dateStarted;

    private Date dateFinished;

    private String error;

    private String config;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public List<TreeNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(final List<TreeNodeVO> nodes) {
        this.nodes = nodes;
    }

    public Date getDatePlanned() {
        return datePlanned;
    }

    public void setDatePlanned(final Date datePlanned) {
        this.datePlanned = datePlanned;
    }

    public Date getDateStarted() {
        return dateStarted;
    }

    public void setDateStarted(final Date dateStarted) {
        this.dateStarted = dateStarted;
    }

    public Date getDateFinished() {
        return dateFinished;
    }

    public void setDateFinished(final Date dateFinished) {
        this.dateFinished = dateFinished;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
