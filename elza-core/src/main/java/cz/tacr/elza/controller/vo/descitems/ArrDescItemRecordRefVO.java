package cz.tacr.elza.controller.vo.descitems;


import cz.tacr.elza.controller.vo.RegRecordVO;


/**
 * VO hodnoty atributu - record.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemRecordRefVO extends ArrDescItemVO {

    /**
     * rejstřík
     */
    private RegRecordVO record;

    public RegRecordVO getRecord() {
        return record;
    }

    public void setRecord(final RegRecordVO record) {
        this.record = record;
    }
}