package cz.tacr.elza.drools.model;

import java.util.List;

public class Ap {

    private Integer id;
    private String aeType;
    private List<Part> parts;

    public Ap(final Integer id, final String aeType, final List<Part> parts) {
        this.id = id;
        this.aeType = aeType;
        this.parts = parts;
    }

    public Integer getId() {
        return id;
    }

    public String getAeType() {
        return aeType;
    }

    public List<Part> getParts() {
        return parts;
    }
}
