package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.ArrStructureDataVO;


/**
 * VO hodnoty atributu - structure data.
 *
 * @since 16.11.2017
 */
public class ArrItemStructureVO extends ArrItemVO {

    /**
     * obal
     */
    private Integer value;

    private ArrStructureDataVO structureData;

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    public ArrStructureDataVO getStructureData() {
        return structureData;
    }

    public void setStructureData(final ArrStructureDataVO structureData) {
        this.structureData = structureData;
    }
}
