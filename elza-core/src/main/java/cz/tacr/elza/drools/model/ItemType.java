package cz.tacr.elza.drools.model;

import java.util.HashSet;
import java.util.Set;

public class ItemType {

    private Integer id;
    private String code;
    private boolean repeatable;
    private RequiredType requiredType;
    private Set<ItemSpec> specs;

    public ItemType(final Integer id, final String code) {
        this.id = id;
        this.code = code;
        this.repeatable = false;
        this.requiredType = RequiredType.IMPOSSIBLE;
        this.specs = new HashSet<>();
    }

    public ItemType(final Integer id, final String code, final Set<ItemSpec> specs) {
        this.id = id;
        this.code = code;
        this.repeatable = false;
        this.requiredType = RequiredType.IMPOSSIBLE;
        this.specs = specs;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final boolean repeatable) {
        this.repeatable = repeatable;
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

    public Set<ItemSpec> getSpecs() {
        return specs;
    }
}
