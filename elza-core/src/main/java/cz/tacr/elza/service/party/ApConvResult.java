package cz.tacr.elza.service.party;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApName;

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

    /**
     * Creates new AP description. AP reference is not set.
     */
    public ApDescription createDesc() {
        if(description == null) {
            return null;
        }

        ApDescription entity = new ApDescription();
        entity.setDescription(description);
        return entity;
    }

    /**
     * Creates new AP names. AP reference is not set.
     */
    public List<ApName> createNames() {
        Iterator<ApConvName> it = names.iterator();
        Validate.isTrue(it.hasNext());

        List<ApName> result = new ArrayList<>(names.size());

        ApName prefName = createName(it.next());
        prefName.setPreferredName(true);
        result.add(prefName);

        while (it.hasNext()) {
            ApName name = createName(it.next());
            result.add(name);
        }
        return result;
    }

    private ApName createName(ApConvName name) {
        Validate.notBlank(name.getName());

        ApName entity = new ApName();
        entity.setComplement(name.getComplement());
        entity.setLanguage(name.getLanguage());
        entity.setName(name.getName());
        entity.setFullName(AccessPointDataService.generateFullName(name.getName(), name.getComplement()));
        return entity;
    }
}
