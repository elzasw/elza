package cz.tacr.elza.service.merge;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ApPart;

public class PartWithSubParts {

    private ApPart part;
    private List<ApPart> subParts = new ArrayList<>();

    public ApPart getPart() {
        return part;
    }

    public void setPart(ApPart part) {
        this.part = part;
    }

    public List<ApPart> getSubParts() {
        return subParts;
    }

    public void addSubPart(ApPart subPart) {
        subParts.add(subPart);
    }
}
