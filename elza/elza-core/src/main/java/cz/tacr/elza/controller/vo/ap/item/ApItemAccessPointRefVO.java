package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.AccessPointItem;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;

import java.util.Objects;

import jakarta.persistence.EntityManager;

public class ApItemAccessPointRefVO extends ApItemVO {

    /**
     * Přístupový bod
     */
    private ApAccessPointVO accessPoint;

    private Integer value;

    private String externalUrl;

    private String externalName;

    public ApItemAccessPointRefVO() {
    }

    public ApItemAccessPointRefVO(final AccessPointItem item, final GetExternalUrl getExternalUrl) {
        super(item);
        ArrDataRecordRef data = HibernateUtils.unproxy(item.getData());
        if (data != null) {
            ApBinding binding = data.getBinding();
            if (binding != null) {
                setExternalName(binding.getValue());
                setExternalUrl(getExternalUrl.getUrl(binding.getExternalSystemId(), binding.getValue()));
            }
            value = data.getRecordId();
        }
    }

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

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(final String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(final String externalName) {
        this.externalName = externalName;
    }

    // Entity can be created only from ID and not from embedded object
    @Override
    public ArrData createDataEntity(EntityManager em) {
        ArrDataRecordRef data = new ArrDataRecordRef();

        // try to map accessPoint
        ApAccessPoint accessPoint = null;
        if (this.value != null) {
            accessPoint = em.getReference(ApAccessPoint.class, value);
        }
        data.setRecord(accessPoint);

        data.setDataType(DataType.RECORD_REF.getEntity());
        return data;
    }

    @FunctionalInterface
    public interface GetExternalUrl {
        String getUrl(Integer externalSystemId, String value);
    }

    @Override
    public boolean equalsValue(AccessPointItem item) {
        ArrDataRecordRef data = (ArrDataRecordRef) item.getData();
        return equalsBase(item) && Objects.equals(value, data.getRecordId());
    }
}
