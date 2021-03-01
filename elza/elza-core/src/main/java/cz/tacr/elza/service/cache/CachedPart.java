package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RulPartType;
import java.util.List;

public class CachedPart implements AccessPointCacheSerializable {

    private Integer partId;

    private ApStateEnum state;

    private String errorDescription;

    private RulPartType partType;

    private Integer parentPartId;

    private ApChange createChange;

    private ApChange deleteChange;

    private ApKeyValue keyValue;

    private List<ApItem> items;

    private List<ApIndex> indices;

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(Integer partId) {
        this.partId = partId;
    }

    public ApStateEnum getState() {
        return state;
    }

    public void setState(ApStateEnum state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public RulPartType getPartType() {
        return partType;
    }

    public void setPartType(RulPartType partType) {
        this.partType = partType;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParentPartId(Integer parentPartId) {
        this.parentPartId = parentPartId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ApKeyValue getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(ApKeyValue keyValue) {
        this.keyValue = keyValue;
    }

    public List<ApItem> getItems() {
        return items;
    }

    public void setItems(List<ApItem> items) {
        this.items = items;
    }

    public List<ApIndex> getIndices() {
        return indices;
    }

    public void setIndices(List<ApIndex> indices) {
        this.indices = indices;
    }
}
