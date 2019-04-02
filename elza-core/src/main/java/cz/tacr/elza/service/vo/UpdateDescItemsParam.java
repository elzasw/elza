package cz.tacr.elza.service.vo;

import java.util.List;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;

/**
 * Předávané parametry pro hromadnou úpravu JP.
 *
 * @since 12.04.2018
 */
public class UpdateDescItemsParam {

    private List<ArrItemVO> createItemVOs;
    private List<ArrItemVO> updateItemVOs;
    private List<ArrItemVO> deleteItemVOs;

    public UpdateDescItemsParam(final List<ArrItemVO> createItemVOs,
                                final List<ArrItemVO> updateItemVOs,
                                final List<ArrItemVO> deleteItemVOs) {
        this.createItemVOs = createItemVOs;
        this.updateItemVOs = updateItemVOs;
        this.deleteItemVOs = deleteItemVOs;
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
