package cz.tacr.elza.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MultipleApChangeContext {

    private Set<Integer> accessPointIds = new HashSet<>(); 

    public void add(Integer accessPointId) {
        accessPointIds.add(accessPointId);
    }

    public Collection<Integer> getModifiedApIds() {
        return accessPointIds;
    }
}
