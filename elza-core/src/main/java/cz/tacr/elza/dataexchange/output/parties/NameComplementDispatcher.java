package cz.tacr.elza.dataexchange.output.parties;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.dataexchange.output.loaders.NestedLoadDispatcher;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPartyNameComplement;

public class NameComplementDispatcher extends NestedLoadDispatcher<ParPartyNameComplement> {

    private final List<ParPartyNameComplement> nameComplements = new ArrayList<>();

    private final ParPartyName name;

    public NameComplementDispatcher(ParPartyName name, LoadDispatcher<ParPartyName> nameDispatcher) {
        super(nameDispatcher);
        this.name = name;
    }

    @Override
    public void onLoad(ParPartyNameComplement result) {
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
