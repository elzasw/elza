package cz.tacr.elza.service.vo;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;

import java.util.List;

/**
 * Předávané parametry pro hromadnou úpravu JP.
 *
 * @since 12.04.2018
 */
public class UpdateDescItemsParam {

    private Integer nodeId;
    private Integer nodeVersion;
    private List<ArrItemVO> createItemVOs;
    private List<ArrItemVO> updateItemVOs;
    private List<ArrItemVO> deleteItemVOs;

    public UpdateDescItemsParam(final Integer nodeId,
                                final Integer nodeVersion,
                                final List<ArrItemVO> createItemVOs,
                                final List<ArrItemVO> updateItemVOs,
                                final List<ArrItemVO> deleteItemVOs) {
        this.nodeId = nodeId;
        this.nodeVersion = nodeVersion;
        this.createItemVOs = createItemVOs;
        this.updateItemVOs = updateItemVOs;
        this.deleteItemVOs = deleteItemVOs;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public Integer getNodeVersion() {
        return nodeVersion;
    }

    public List<ArrItemVO> getCreateItemVOs() {
        return createItemVOs;
    }

    public List<ArrItemVO> getUpdateItemVOs() {
        return updateItemVOs;
    }

    public List<ArrItemVO> getDeleteItemVOs() {
        return deleteItemVOs;
    }
}
