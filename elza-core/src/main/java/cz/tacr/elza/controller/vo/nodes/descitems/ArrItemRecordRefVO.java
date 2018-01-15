package cz.tacr.elza.controller.vo.nodes.descitems;


import javax.persistence.EntityManager;

import cz.tacr.elza.controller.vo.RegRecordVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


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

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataRecordRef data = new ArrDataRecordRef();

        if (record != null) {
            throw new BusinessException("Inconsistent data, record is not null", BaseCode.PROPERTY_IS_INVALID);
        }

        // try to map record
        RegRecord record = null;
        if (this.value != null) {
            record = em.getReference(RegRecord.class, value);
        }
        data.setRecord(record);

        data.setDataType(DataType.RECORD_REF.getEntity());
        return data;
    }
}