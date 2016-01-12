package cz.tacr.elza.controller.vo.descitems;

import java.util.List;

import cz.tacr.elza.controller.vo.RulDescItemConstraintVO;
import cz.tacr.elza.controller.vo.RulDescItemSpecVO;


/**
 * VO typu atributu
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class RulDescItemTypeVO {

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
    private List<ArrDescItemVO> descItems;

    /**
     * seznam specifikací
     */
    private List<RulDescItemSpecVO> descItemSpecs;

    /**
     * seznam omezení
     */
    private List<RulDescItemConstraintVO> descItemConstraints;

    /**
     * šířka atributu (0 - maximální počet sloupců, 1..N - počet sloupců)
     */
    private Integer width;

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

    public List<RulDescItemSpecVO> getDescItemSpecs() {
        return descItemSpecs;
    }

    public void setDescItemSpecs(final List<RulDescItemSpecVO> descItemSpecs) {
        this.descItemSpecs = descItemSpecs;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    public List<RulDescItemConstraintVO> getDescItemConstraints() {
        return descItemConstraints;
    }

    public void setDescItemConstraints(final List<RulDescItemConstraintVO> descItemConstraints) {
        this.descItemConstraints = descItemConstraints;
    }
}
