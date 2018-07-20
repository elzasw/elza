package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;

public class ApAccessPointData {

    private final LinkedList<ApName> names = new LinkedList<>();

    private final List<ApExternalId> externalIds = new ArrayList<>();

    private ApAccessPoint accessPoint;

    private ApDescription description;

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    public Integer getAccessPointId() {
        return accessPoint.getAccessPointId();
    }

    public ApDescription getDescription() {
        return description;
    }

    public void setDescription(ApDescription description) {
        this.description = description;
    }

    public List<ApName> getNames() {
        return names;
    }

    public void addName(ApName name) {
        if (name.isPreferredName()) {
            Validate.isTrue(names.isEmpty() || !names.getFirst().isPreferredName());
            names.addFirst(name);
        } else {
            names.add(name);
        }
    }
    
    public ApName getPreferredName() {
        if (names.size() > 0) {
            ApName name = names.getFirst();
            if (name.isPreferredName()) {
                return name;
            }
        }
        return null;
    }

    public List<ApExternalId> getExternalIds() {
        return externalIds;
    }

    public void addExternalId(ApExternalId externalId) {
        externalIds.add(externalId);
    }
}
