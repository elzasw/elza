package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ParPartyName;

public class NameLoader extends AbstractEntityLoader<Integer, ParPartyName> {

    private final NameComplementLoader complementLoader;

    private final UnitdateLoader unitdateLoader;

    public NameLoader(EntityManager em, int batchSize, UnitdateLoader unitdateLoader) {
        super(ParPartyName.class, ParPartyName.PARTY_FK, em, batchSize);
        this.complementLoader = new NameComplementLoader(em, batchSize);
        this.unitdateLoader = unitdateLoader;
    }

    @Override
    public void flush() {
        super.flush();
        complementLoader.flush();
    }

    @Override
    protected void onRequestLoad(ParPartyName result, LoadDispatcher<ParPartyName> dispatcher) {
        NameComplementDispatcher complementDispatcher = new NameComplementDispatcher(result, dispatcher);
        complementLoader.addRequest(result.getPartyNameId(), complementDispatcher);

        if (result.getValidFromUnitdateId() != null) {
            UnitdateDispatcher unitdateDispatcher = new UnitdateDispatcher(dispatcher) {
                @Override
                protected void onCompleted() {
                    result.setValidFrom(Validate.notNull(getUnitdate()));
                }
            };
            unitdateLoader.addRequest(result.getValidFromUnitdateId(), unitdateDispatcher);
        }
        if (result.getValidToUnitdateId() != null) {
            UnitdateDispatcher unitdateDispatcher = new UnitdateDispatcher(dispatcher) {
                @Override
                protected void onCompleted() {
                    result.setValidTo(Validate.notNull(getUnitdate()));
                }
            };
            unitdateLoader.addRequest(result.getValidToUnitdateId(), unitdateDispatcher);
        }
    }
}
