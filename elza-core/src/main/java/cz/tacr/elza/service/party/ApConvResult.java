package cz.tacr.elza.service.party;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ApChange;
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
    public ApDescription createDesc(ApChange createChange) {
        Validate.notBlank(description);

        ApDescription entity = new ApDescription();
        entity.setCreateChange(createChange);
        entity.setDescription(description);
        return entity;
    }

    /**
     * Creates new AP names. AP reference is not set.
     */
    public void createNames(ApChange createChange, Consumer<ApName> nameAction) {
        Iterator<ApConvName> it = names.iterator();
        Validate.isTrue(it.hasNext());
        ApName prefName = createName(it.next(), createChange);
        prefName.setPreferredName(true);
        nameAction.accept(prefName);
        while (it.hasNext()) {
            ApName name = createName(it.next(), createChange);
            nameAction.accept(name);
        }
    }

    private ApName createName(ApConvName name, ApChange createChange) {
        Validate.notBlank(name.getName());

        ApName entity = new ApName();
        entity.setComplement(name.getComplement());
        entity.setCreateChange(createChange);
        entity.setLanguage(name.getLanguage());
        entity.setName(name.getName());
        entity.setNameType(name.getType());
        return entity;
    }
}
