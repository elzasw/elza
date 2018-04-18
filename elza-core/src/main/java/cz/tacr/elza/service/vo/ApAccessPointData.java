package cz.tacr.elza.service.vo;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;

import java.util.List;

public class ApAccessPointData {
    private ApExternalId externalId;
    private ApName preferredName;
    private ApDescription characteristics;
    private List<ApName> variantRecordList;
    private ApExternalSystem externalSystem;
    private ApAccessPoint accessPoint;

    public ApAccessPointData(){
    }

    public ApAccessPointData(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    public ApExternalId getExternalId() {
        return externalId;
    }

    public void setExternalId(ApExternalId externalId) {
        this.externalId = externalId;
    }

    public Integer getAccessPointId() {
        return accessPoint.getAccessPointId();
    }

    public ApName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(ApName preferredName) {
        this.preferredName = preferredName;
        this.preferredName.setPreferredName(Boolean.TRUE);
    }

    public ApDescription getDescription() {
        return characteristics;
    }

    public List<ApName> getVariantRecordList() {
        return variantRecordList;
    }

    public void setVariantNameList(List<ApName> variantRecordList) {
        this.variantRecordList = variantRecordList;
    }

    public ApExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(ApExternalSystem externalSystem) {
        this.externalSystem = externalSystem;
    }

    public void setCharacteristics(ApDescription characteristics) {
        this.characteristics = characteristics;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }
}
