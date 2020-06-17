package cz.tacr.elza.drools.model;

import java.util.ArrayList;
import java.util.List;

public class Relation {

    private List<Part> parts;

    private Integer relationCount;

    public Relation(Part part) {
        this.parts = new ArrayList<>();
        this.parts.add(part);
        this.relationCount = 1;
    }

    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public Integer getRelationCount() {
        return relationCount;
    }

    public void setRelationCount(Integer relationCount) {
        this.relationCount = relationCount;
    }

    public void addPart(Part part) {
        this.parts.add(part);
        this.relationCount++;
    }
}
