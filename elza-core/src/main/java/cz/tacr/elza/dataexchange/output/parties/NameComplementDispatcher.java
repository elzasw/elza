package cz.tacr.elza.dataexchange.output.parties;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParComplementType;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class NameComplementDispatcher extends NestedLoadDispatcher<ParPartyNameComplement> {

    // --- fields ---

    private final List<ParPartyNameComplement> nameComplements = new ArrayList<>();

    private final ParPartyName name;

    private final StaticDataProvider staticData;

    // --- constructor ---

    public NameComplementDispatcher(ParPartyName name, LoadDispatcher<ParPartyName> nameDispatcher, StaticDataProvider staticData) {
        super(nameDispatcher);
        this.name = name;
        this.staticData = staticData;
    }

    // --- methods ---

    @Override
    public void onLoad(ParPartyNameComplement result) {
        // init complement type
        ParComplementType cmplType = staticData.getCmplTypeById(result.getComplementTypeId());
        Validate.notNull(cmplType);
        result.setComplementType(cmplType);
        // init references
        nameComplements.add(result);
    }

    @Override
    public void onCompleted() {
        if (nameComplements.isEmpty()) {
            name.setPartyNameComplements(null);
        } else {
            name.setPartyNameComplements(nameComplements);
        }
    }
}
