package cz.tacr.elza.controller.vo.nodes;

import java.util.List;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrDescItemVO;


/**
 * VO Odlehčená verze typu hodnoty atributu.
 *
 * @author Martin Šlapa
 * @since 11.2.2016
 */
public class DescItemTypeDescItemsLiteVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * seznam hodnot atributu
     */
    private List<ArrDescItemVO> descItems;

    private Integer viewOrder;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public List<ArrDescItemVO> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<ArrDescItemVO> descItems) {
        this.descItems = descItems;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }
}
