package cz.tacr.elza.domain;

import java.util.Objects;

public class ArrItemUriRef  extends ArrItemData {

    private String schema;

    private String value;

    private String description;

    private ArrNode node;

    public ArrNode getNode() {
        return node;
    }

    public void setNode(ArrNode node) {
        this.node = node;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return value + " ; " + description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemUriRef that = (ArrItemUriRef) o;
        return Objects.equals(schema, that.schema) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), schema, value);
    }
}
