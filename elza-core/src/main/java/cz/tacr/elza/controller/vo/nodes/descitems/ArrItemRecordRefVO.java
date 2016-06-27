package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.RegRecordVO;


/**
 * VO hodnoty atributu - record.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrItemRecordRefVO extends ArrItemVO {

    /**
     * rejstřík
     */
    private RegRecordVO record;

    private Integer value;

    public RegRecordVO getRecord() {
        return record;
    }

    public void setRecord(final RegRecordVO record) {
        this.record = record;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }
}