package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.ArrNodeConformity;
import cz.tacr.elza.domain.ArrNodeConformityExt;

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

    /**
     * Mapa zobrazování chyb/chybějících.
     */
    private Map<Integer, Boolean> policyTypeIdsVisible;

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

    public Map<Integer, Boolean> getPolicyTypeIdsVisible() {
        return policyTypeIdsVisible;
    }

    public void setPolicyTypeIdsVisible(final Map<Integer, Boolean> policyTypeIdsVisible) {
        this.policyTypeIdsVisible = policyTypeIdsVisible;
    }
    
    public static NodeConformityVO newInstance(final ArrNodeConformityExt nodeConformity) {
    	NodeConformityVO result = new NodeConformityVO();
    	result.setNodeId(nodeConformity.getNodeId());
    	result.setState(nodeConformity.getState());
    	result.setDescription(nodeConformity.getDescription());
    	result.setDate(nodeConformity.getDate());
    	// TODO added missingList, errorList
    	return result;
    }
}
