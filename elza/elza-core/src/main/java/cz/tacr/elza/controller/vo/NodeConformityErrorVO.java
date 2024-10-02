package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrNodeConformityError;

/**
 * VO pro chyby z validace.
 */
public class NodeConformityErrorVO {

    /**
     * Identifikátor hodnoty atributu.
     */
    private Integer descItemObjectId;

    /**
     * Popis chyby.
     */
    private String description;

    /**
     * Identifikátor typu oprávnění.
     */
    private Integer policyTypeId;

    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
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

    public static NodeConformityErrorVO newInstance(final ArrNodeConformityError nodeConformityError) {
    	NodeConformityErrorVO result = new NodeConformityErrorVO();
        // TODO: Rework NodeConformityErrorVO to use descItemId and not descItemObjectId
    	result.setDescItemObjectId(nodeConformityError.getDescItem().getDescItemObjectId());
    	result.setDescription(nodeConformityError.getDescription());
    	result.setPolicyTypeId(nodeConformityError.getPolicyTypeId());
    	return result;
    }

    public static List<NodeConformityErrorVO> newInstance(final List<ArrNodeConformityError> mces) {
    	if (mces == null) {
    		return null;
    	}
    	return mces.stream().map(i -> NodeConformityErrorVO.newInstance(i)).toList();
    }
}
