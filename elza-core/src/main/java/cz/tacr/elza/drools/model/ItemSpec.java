package cz.tacr.elza.drools.model;

import cz.tacr.elza.domain.RulItemSpec;

public class ItemSpec {

    final private RulItemSpec itemSpec;
    private boolean repeatable;
    private RequiredType requiredType;

    public ItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
        this.repeatable = false;
        this.requiredType = RequiredType.IMPOSSIBLE;
    }

    public String getCode() {
        return itemSpec.getCode();
    }

    public RequiredType getRequiredType() {
        return requiredType;
    }

    public void setRequired() {
        requiredType = RequiredType.REQUIRED;
    }

    public void setPossible() {
        requiredType = RequiredType.POSSIBLE;
    }

    public void setImpossible() {
        requiredType = RequiredType.IMPOSSIBLE;
    }

    public void setRequiredType(final RequiredType requiredType) {
        this.requiredType = requiredType;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }
}
