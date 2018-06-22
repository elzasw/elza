package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ParPartyName;

public class NameLoader extends AbstractEntityLoader<ParPartyName> {

    private final NameComplementLoader complementLoader;

    private final UnitdateLoader unitdateLoader;

    private final StaticDataProvider staticData;

    public NameLoader(EntityManager em, int batchSize, UnitdateLoader unitdateLoader, StaticDataProvider staticData) {
        super(ParPartyName.class, ParPartyName.PARTY_FK, em, batchSize);
        this.complementLoader = new NameComplementLoader(em, batchSize);
        this.unitdateLoader = unitdateLoader;
        this.staticData = staticData;
    }

    @Override
    public void flush() {
        super.flush();
        complementLoader.flush();
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<ParPartyName> dispatcher, ParPartyName result) {
        NameComplementDispatcher complementDispatcher = new NameComplementDispatcher(result, dispatcher, staticData);
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
