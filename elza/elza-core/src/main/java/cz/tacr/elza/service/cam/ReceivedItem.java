package cz.tacr.elza.service.cam;

import java.util.List;

import com.google.common.base.Objects;

import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class ReceivedItem {

    private final RulItemType itemType;

    private final RulItemSpec itemSpec;

    private final String uuid;

    private final ArrData data;

    private Integer itemId;
    
    public ReceivedItem(RulItemType itemType, RulItemSpec itemSpec, String uuid, ArrData data) {
        this.itemType = itemType;
        this.itemSpec = itemSpec;
        this.uuid = uuid;
        this.data = data;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public String getUuid() {
        return uuid;
    }

    public ArrData getData() {
        return data;
    }

    public Integer getItemId() {
        return itemId;
    }

    public boolean contains(List<ApItem> items) {
        for (ApItem item : items) {
            if (compare(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean compare(ApItem item) {
        if (!itemType.getItemTypeId().equals(item.getItemTypeId())) {
            return false;
        }

        if (!Objects.equal(itemSpec, item.getItemSpec())) {
            return false;
        }

        if (!data.isEqualValue(item.getData())) {
            return false;
        }

        itemId = item.getItemId();

        return true;
    }
}
