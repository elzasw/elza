package cz.tacr.elza.service.party;

import java.util.ArrayList;
import java.util.List;

public class ApConvResult {

    private final List<ApConvName> names = new ArrayList<>();

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addName(ApConvName name) {
        names.add(name);
    }

    public List<ApConvName> getNames() {
        return names;
    }

}
