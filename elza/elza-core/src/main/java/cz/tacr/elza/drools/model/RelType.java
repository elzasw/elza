package cz.tacr.elza.drools.model;

public class RelType {

    private Integer id;
    private String code;
    private boolean repeatable;
    private RequiredType requiredType;

    public RelType(final Integer id, final String code) {
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

    public void setRequiredType(final RequiredType requiredType) {
        this.requiredType = requiredType;
    }
}
