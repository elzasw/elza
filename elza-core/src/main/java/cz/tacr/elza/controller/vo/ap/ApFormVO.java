package cz.tacr.elza.controller.vo.ap;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.controller.vo.nodes.ItemTypeLiteVO;

import java.util.List;

/**
 * @since 18.07.2018
 */
public class ApFormVO {

    /**
     * seznam hodnot atributu
     */
    private List<ApItemVO> items;

    /**
     * typy atributů (všechny kromě nemožných)
     */
    private List<ItemTypeLiteVO> itemTypes;

    public ApFormVO() {
    }

    public ApFormVO(final List<ApItemVO> items,
                    final List<ItemTypeLiteVO> itemTypes) {
        this.items = items;
        this.itemTypes = itemTypes;
    }

    public List<ApItemVO> getItems() {
        return items;
    }

    public void setItems(final List<ApItemVO> items) {
        this.items = items;
    }

    public List<ItemTypeLiteVO> getItemTypes() {
        return itemTypes;
    }

    public void setItemTypes(final List<ItemTypeLiteVO> itemTypes) {
        this.itemTypes = itemTypes;
    }

}
