package cz.tacr.elza.drools.model;

import java.util.List;

// TODO: Consider renaming class to ModelPartType
public class ModelPart {

    private PartType type;

    private boolean repeatable;

    private List<Index> indices;

    public ModelPart(final PartType type, final List<Index> indices) {
        this.type = type;
        this.indices = indices;
        this.repeatable = false;
    }

    public PartType getType() {
        return type;
    }

    public void setType(PartType type) {
        this.type = type;
    }

    public List<Index> getIndices() {
        return indices;
    }

    public void setIndices(List<Index> indices) {
        this.indices = indices;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
}
