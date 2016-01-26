package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;

import cz.tacr.elza.api.ArrNodeConformity;

/**
 * VO pro validace JP.
 *
 * @author Martin Šlapa
 * @since 26.1.2016
 */
public class NodeConformityVO {

    /**
     * Identifikátor JP.
     */
    private Integer nodeId;

    /**
     * Stav JP.
     */
    private ArrNodeConformity.State state;

    /**
     * Popis validace.
     */
    private String description;

    /**
     * Datum validace.
     */
    private Date date;

    /**
     * Seznam chybějících hodnot.
     */
    private List<NodeConformityMissingVO> missingList;

    /**
     * Seznam chybných hodnot.
     */
    private List<NodeConformityErrorVO> errorList;

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public ArrNodeConformity.State getState() {
        return state;
    }

    public void setState(final ArrNodeConformity.State state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public List<NodeConformityMissingVO> getMissingList() {
        return missingList;
    }

    public void setMissingList(final List<NodeConformityMissingVO> missingList) {
        this.missingList = missingList;
    }

    public List<NodeConformityErrorVO> getErrorList() {
        return errorList;
    }

    public void setErrorList(final List<NodeConformityErrorVO> errorList) {
        this.errorList = errorList;
    }
}
