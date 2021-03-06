package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;

public class ApAggregationItemConfig {
    boolean fromPrefferedName = false;

    List<String> fromPart;

    String fromType;

    String toItem;

    boolean group = false;

    String groupSeparator = "";

    public ApAggregationItemConfig() {
    }

    public boolean isFromPrefferedName() {
        return fromPrefferedName;
    }

    public void setFromPrefferedName(boolean fromPrefferedName) {
        this.fromPrefferedName = fromPrefferedName;
    }

    public List<String> getFromPart() {
        return fromPart;
    }

    public void setFromPart(List<String> fromPart) {
        this.fromPart = fromPart;
    }

    public String getFromType() {
        return fromType;
    }

    public void setFromType(String fromType) {
        this.fromType = fromType;
    }

    public String getToItem() {
        return toItem;
    }

    public void setToItem(String toItem) {
        this.toItem = toItem;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public String getGroupSeparator() {
        return groupSeparator;
    }

    public void setGroupSeparator(String groupSeparator) {
        this.groupSeparator = groupSeparator;
    }
}
