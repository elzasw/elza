package cz.tacr.elza.drools.model;

public class ItemSpec {

    private Integer id;
    private String code;
    private boolean repeatable;
    private RequiredType requiredType;

    public ItemSpec(final Integer id, final String code) {
        this.id = id;
        this.code = code;
        this.repeatable = false;
        this.requiredType = RequiredType.IMPOSSIBLE;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
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
}
