package cz.tacr.elza.controller.vo.ap.item;

import cz.tacr.elza.controller.vo.ApAccessPointVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.*;

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

    private String externalUrl;

    private String externalName;

    public ApItemAccessPointRefVO() {
    }

    public ApItemAccessPointRefVO(final ApItem item, final GetExternalUrl getExternalUrl) {
        super(item);
        ArrDataRecordRef data = (ArrDataRecordRef) item.getData();
        if (data != null) {
            ApBinding binding = data.getBinding();
            if (binding != null) {
                setExternalName(binding.getValue());
                setExternalUrl(getExternalUrl.getUrl(binding.getExternalSystemId(), binding.getValue()));
            }
        }
        value = data == null ? null : data.getRecordId();;
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
}
