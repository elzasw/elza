package cz.tacr.elza.drools.model;

public class Index {

    private String indexType;

    private String value;

    private Part part;

    private boolean repeatable;

    public Index(final String indexType, final String value, final Part part) {
        this.indexType = indexType;
        this.value = value;
        this.part = part;
        this.repeatable = false;
    }

    public String getIndexType() {
        return indexType;
    }

    public void setIndexType(String indexType) {
        this.indexType = indexType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
}
