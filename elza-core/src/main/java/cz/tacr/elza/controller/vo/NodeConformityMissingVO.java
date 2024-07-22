package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrNodeConformityMissing;

/**
 * VO pro chybějící položky z validace.
 */
public class NodeConformityMissingVO {

    /**
     * Identifikátor typu.
     */
    private Integer descItemTypeId;

    /**
     * Identifikátor specifikace.
     */
    private Integer descItemSpecId;

    /**
     * Popis chybějící položky.
     */
    private String description;

    /**
     * Identifikátor typu oprávnění.
     */
    private Integer policyTypeId;

    public Integer getDescItemTypeId() {
        return descItemTypeId;
    }

    public void setDescItemTypeId(final Integer descItemTypeId) {
        this.descItemTypeId = descItemTypeId;
    }

    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getPolicyTypeId() {
        return policyTypeId;
    }

    public void setPolicyTypeId(final Integer policyTypeId) {
        this.policyTypeId = policyTypeId;
    }

    public static NodeConformityMissingVO newInstance(final ArrNodeConformityMissing nodeConformityMissing) {
    	NodeConformityMissingVO result = new NodeConformityMissingVO();
    	result.setDescItemSpecId(nodeConformityMissing.getItemSpecId());
    	result.setDescItemTypeId(nodeConformityMissing.getItemTypeId());
    	result.setDescription(nodeConformityMissing.getDescription());
    	result.setPolicyTypeId(nodeConformityMissing.getPolicyTypeId());
    	return result;
    }
    
    public static List<NodeConformityMissingVO> newInstance(final List<ArrNodeConformityMissing> ncms) {
    	if (ncms == null) {
    		return null;
    	}
    	return ncms.stream().map(i -> NodeConformityMissingVO.newInstance(i)).toList();
    }
}
