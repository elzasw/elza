package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;
import javax.persistence.criteria.FetchParent;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyType;

public class PartyLoader extends AbstractEntityLoader<ParParty> {

    private final UnitdateLoader unitdateLoader;

    private final NameLoader nameLoader;

    private final PartyGroupIndentifierLoader groupIndentifierLoader;

    private final StaticDataProvider staticData;

    private PartyLoader(String subEntityQueryIdPath, EntityManager em, int batchSize, StaticDataProvider staticData) {
        super(ParParty.class, subEntityQueryIdPath, em, batchSize);
        this.unitdateLoader = new UnitdateLoader(em, batchSize);
        this.nameLoader = new NameLoader(em, batchSize, unitdateLoader);
        this.groupIndentifierLoader = new PartyGroupIndentifierLoader(em, batchSize, unitdateLoader);
        this.staticData = staticData;
    }

    @Override
    protected void setEntityFetch(FetchParent<?, ?> baseEntity) {
        baseEntity.fetch(ParParty.RECORD);
    }

    @Override
    public void flush() {
        super.flush();
        nameLoader.flush();
        groupIndentifierLoader.flush();
        // flush unit-date after name and identifier loader!
        unitdateLoader.flush();
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<ParParty> dispatcher, ParParty result) {
        prepareCachedRelations(result);

        NameDispatcher nameDispatcher = new NameDispatcher(result, dispatcher);
        nameLoader.addRequest(result.getPartyId(), nameDispatcher);

        if (result.getPartyType().toEnum() == PartyType.GROUP_PARTY) {
            PartyGroupIdentifierDispatcher idDispatcher = new PartyGroupIdentifierDispatcher((ParPartyGroup) result, dispatcher);
            groupIndentifierLoader.addRequest(result.getPartyId(), idDispatcher);
        }
    }

    public static PartyLoader createPartyIdLoader(EntityManager em, int batchSize, StaticDataProvider staticData) {
        return new PartyLoader(ParParty.ABSTRACT_PARTY_ID, em, batchSize, staticData);
    }

    public static PartyLoader createAPIdLoader(EntityManager em, int batchSize, StaticDataProvider staticData) {
        return new PartyLoader(ParParty.RECORD_FK, em, batchSize, staticData);
    }

    private void prepareCachedRelations(ParParty result) {
        Integer regTypeId = result.getRecord().getRegisterTypeId();
        ParPartyType partyType = staticData.getRegisterTypeById(regTypeId).getPartyType();
        Validate.notNull(partyType);
        result.setPartyType(partyType);
    }
}
