package cz.tacr.elza.interpi.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApState;

public class ApAccessPointData {

    // --- fields ---

    private final LinkedList<ApName> names = new LinkedList<>();

    private final List<ApExternalId> externalIds = new ArrayList<>();

    private ApState apState;

    private ApDescription description;

    // --- getters/setters ---

    public ApState getApState() {
        return apState;
    }

    public void setApState(ApState apState) {
        this.apState = apState;
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
