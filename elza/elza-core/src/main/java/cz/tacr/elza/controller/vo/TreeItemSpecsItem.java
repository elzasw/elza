package cz.tacr.elza.controller.vo;

import java.util.List;
import java.util.Objects;

/**
 * Kategorie specifikace.
 *
 * @author Martin Šlapa
 * @since 27.10.2016
 */
public class TreeItemSpecsItem {

    public enum Type {
        GROUP,
        ITEM
    }

    private Type type;

    private Integer specId;

    private String name;

    private List<TreeItemSpecsItem> children;

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(final Integer specId) {
        this.specId = specId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<TreeItemSpecsItem> getChildren() {
        return children;
    }

    public void setChildren(final List<TreeItemSpecsItem> children) {
        this.children = children;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeItemSpecsItem that = (TreeItemSpecsItem) o;
        return type == that.type &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }
}
