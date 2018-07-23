package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;

import javax.persistence.EntityManager;

/**
 * @since 18.07.2018
 */
public class ApItemAccessPointRefVO extends ApItemVO {

    /**
     * přístupový bod
     */
    private ApAccessPointVO accessPoint;

    private Integer value;

    public ApAccessPointVO getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(final ApAccessPointVO accessPoint) {
        this.accessPoint = accessPoint;
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

        if (accessPoint != null) {
            throw new BusinessException("Inconsistent data, accessPoint is not null", BaseCode.PROPERTY_IS_INVALID);
        }

        // try to map accessPoint
        ApAccessPoint accessPoint = null;
        if (this.value != null) {
            accessPoint = em.getReference(ApAccessPoint.class, value);
        }
        data.setRecord(accessPoint);

        data.setDataType(DataType.RECORD_REF.getEntity());
        return data;
    }
}
