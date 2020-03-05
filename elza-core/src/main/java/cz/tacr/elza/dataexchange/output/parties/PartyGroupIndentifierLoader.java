package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;

public class PartyGroupIndentifierLoader extends
        AbstractEntityLoader<ParPartyGroupIdentifier, ParPartyGroupIdentifier> {

    private final UnitdateLoader unitdateLoader;

    public PartyGroupIndentifierLoader(EntityManager em, int batchSize, UnitdateLoader unitdateLoader) {
        super(ParPartyGroupIdentifier.class, ParPartyGroupIdentifier.PARTY_GROUP_FK, em, batchSize);
        this.unitdateLoader = unitdateLoader;
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<ParPartyGroupIdentifier> dispatcher, ParPartyGroupIdentifier result) {
        if (result.getFromUnitdateId() != null) {
            UnitdateDispatcher unitdateDispatcher = new UnitdateDispatcher(dispatcher) {
                @Override
                protected void onCompleted() {
                    result.setFrom(Validate.notNull(getUnitdate()));
                }
            };
            unitdateLoader.addRequest(result.getFromUnitdateId(), unitdateDispatcher);
        }
        if (result.getToUnitdateId() != null) {
            UnitdateDispatcher unitdateDispatcher = new UnitdateDispatcher(dispatcher) {
                @Override
                protected void onCompleted() {
                    result.setTo(Validate.notNull(getUnitdate()));
                }
            };
            unitdateLoader.addRequest(result.getToUnitdateId(), unitdateDispatcher);
        }
    }
}
