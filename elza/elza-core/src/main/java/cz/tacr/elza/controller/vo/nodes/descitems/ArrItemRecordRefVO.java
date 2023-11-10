package cz.tacr.elza.controller.vo.nodes.descitems;


import java.util.Objects;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.persistence.EntityManager;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;


/**
 * VO hodnoty atributu - record.
 *
 * @since 8.1.2016
 */
public class ArrItemRecordRefVO extends ArrItemVO {

    /**
     * rejstřík
     */
    private ApAccessPointVO record;

    private Integer value;

    public ArrItemRecordRefVO() {

    }

    public ArrItemRecordRefVO(ArrItem item, ApAccessPointVO value) {
        super(item);
        this.record = value;
        if (record != null) {
            this.value = record.getId();
        }
    }

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
            if (!Objects.equals(record.getId(), value)) {
                throw new BusinessException("Inconsistent data, party is not null", BaseCode.PROPERTY_IS_INVALID)
                        .set("value", value).set("record.id", record.getId());
            }
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

    public static ArrItemRecordRefVO newInstance(ArrItem item, ApFactory apFactory) {
        ArrData data = HibernateUtils.unproxy(item.getData());
        ApAccessPointVO value = null;
        if (data != null) {
            if (!(data instanceof ArrDataRecordRef)) {
                throw new BusinessException("Inconsistent data type", BaseCode.PROPERTY_IS_INVALID)
                        .set("dataClass", item.getClass());
            }
            ArrDataRecordRef dataRecordRef = (ArrDataRecordRef) data;
            value = apFactory.createVO(dataRecordRef.getRecord());

        }
        ArrItemRecordRefVO vo = new ArrItemRecordRefVO(item, value);
        return vo;
    }
}
