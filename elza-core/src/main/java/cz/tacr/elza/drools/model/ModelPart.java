package cz.tacr.elza.drools.model;

import java.util.Collections;
import java.util.List;

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

    // kvůli kompatibilitě s CAM - jednodušší kopírování pravidel (v ELZA se nepoužívá)
    public List<Index> getIndices() {
        return Collections.emptyList();
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
