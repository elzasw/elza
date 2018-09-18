package cz.tacr.elza.controller.vo.nodes.descitems;


import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;


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
    private ApAccessPointVO record;

    private Integer value;

    public ApAccessPointVO getRecord() {
        return record;
    }

    public void setRecord(final ApAccessPointVO record) {
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
        ApAccessPoint record = null;
        if (this.value != null) {
            record = em.getReference(ApAccessPoint.class, value);
        }
        data.setRecord(record);

        data.setDataType(DataType.RECORD_REF.getEntity());
        return data;
    }
}
