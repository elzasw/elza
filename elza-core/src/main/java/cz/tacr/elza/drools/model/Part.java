package cz.tacr.elza.drools.model;


import cz.tacr.elza.drools.model.item.AbstractItem;
import java.util.List;

public class Part {

    private Integer id;
    private Integer parentPartId;
    private PartType type;
    private List<AbstractItem> items;
    private Part parent;
    private boolean childrenRel;
    private boolean preferred;

    public Part(final Integer id, final Integer parentPartId, final PartType type, final List<AbstractItem> items,
                final Part parent, final boolean preferred) {
        this.id = id;
        this.parentPartId = parentPartId;
        this.type = type;
        this.items = items;
        this.parent = parent;
        this.preferred = preferred;
        this.childrenRel = false;
    }

    public PartType getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public List<AbstractItem> getItems() {
        return items;
    }

    public Part getParent() {
        return parent;
    }

    public void setParent(Part parent) {
        this.parent = parent;
    }

    public void setChildrenRel() {
        childrenRel = true;
    }

    public boolean isChildrenRel() {
        return childrenRel;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }
}
