package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class ApAggregationItemsConfig {
    boolean fromPrefferedName = false;

    List<String> fromPart;

    List<String> fromType;

    String toItem;

    String groupSeparator = "";

    public ApAggregationItemsConfig() {
    }

    public boolean isFromPrefferedName() {
        return fromPrefferedName;
    }

    public void setFromPrefferedName(boolean fromPrefferedName) {
        this.fromPrefferedName = fromPrefferedName;
    }

     public String getToItem() {
        return toItem;
    }

    public void setToItem(String toItem) {
        this.toItem = toItem;
    }

    public List<String> getFromPart() {
        return fromPart;
    }

    public void setFromPart(List<String> fromPart) {
        this.fromPart = fromPart;
    }

    public List<String> getFromType() {
        return fromType;
    }

    public void setFromType(List<String> fromType) {
        this.fromType = fromType;
    }

    public String getGroupSeparator() {
        return groupSeparator;
    }

    public void setGroupSeparator(String groupSeparator) {
        this.groupSeparator = groupSeparator;
    }
}
