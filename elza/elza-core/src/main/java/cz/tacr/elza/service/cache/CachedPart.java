package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApStateEnum;

import java.util.ArrayList;
import java.util.List;

public class CachedPart implements AccessPointCacheSerializable {

    private Integer partId;

    private ApStateEnum state;

    private String errorDescription;

    private String partTypeCode;

    private Integer parentPartId;

    private Integer createChangeId;

    private Integer lastChangeId;

    private Integer deleteChangeId;

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

    public String getPartTypeCode() {
        return partTypeCode;
    }

    public void setPartTypeCode(String partTypeCode) {
        this.partTypeCode = partTypeCode;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParentPartId(Integer parentPartId) {
        this.parentPartId = parentPartId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public Integer getLastChangeId() {
        return lastChangeId;
    }

    public void setLastChangeId(Integer lastChangeId) {
        this.lastChangeId = lastChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setDeleteChangeId(Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
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

    public void addItem(ApItem item) {
        if (this.items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    public void addIndex(ApIndex index) {
        if (this.indices == null) {
            indices = new ArrayList<>();
        }
        indices.add(index);
    }
}
