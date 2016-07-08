package cz.tacr.elza.controller.vo.nodes;

import java.util.List;

import cz.tacr.elza.controller.vo.nodes.descitems.ArrItemVO;


/**
 * VO typu atributu
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class RulDescItemTypeDescItemsVO {

    /**
     * identifikátor
     */
    private String id;

    /**
     * kód
     */
    private String code;

    /**
     * název
     */
    private String name;

    /**
     * řazení
     */
    private Integer viewOrder;

    /**
     * identifikátor datového typu
     */
    private Integer dataTypeId;

    /**
     * seznam hodnot atributu
     */
    private List<ArrItemVO> descItems;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    public void setDataTypeId(final Integer dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    public List<ArrItemVO> getDescItems() {
        return descItems;
    }

    public void setDescItems(final List<ArrItemVO> descItems) {
        this.descItems = descItems;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

}
