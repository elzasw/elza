package cz.tacr.elza.drools.model;

public class ModelPart {

    private PartType type;

    private boolean repeatable;

    public ModelPart(final PartType type) {
        this.type = type;
        this.repeatable = false;
    }

    public PartType getType() {
        return type;
    }

    public void setType(PartType type) {
        this.type = type;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
}
