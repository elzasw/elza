package cz.tacr.elza.dataexchange.output.parties;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.aps.ExternalIdDispatcher;
import cz.tacr.elza.dataexchange.output.aps.ExternalIdLoader;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.dataexchange.output.loaders.LoadDispatcher;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;

public class PartyInfoLoader extends AbstractEntityLoader<PartyInfo, ParParty> {

    private final ApStateLoader apStateLoader;

    private final UnitdateLoader unitdateLoader;

    private final NameLoader nameLoader;

    private final PartyGroupIndentifierLoader groupIndentifierLoader;

    private final ExternalIdLoader externalIdLoader;

    private final StaticDataProvider staticData;

    private PartyInfoLoader(String subEntityQueryIdPath, EntityManager em, int batchSize,
            StaticDataProvider staticData) {
        super(ParParty.class, subEntityQueryIdPath, em, batchSize);
        this.apStateLoader = new ApStateLoader(em, batchSize);
        this.unitdateLoader = new UnitdateLoader(em, batchSize);
        this.nameLoader = new NameLoader(em, batchSize, unitdateLoader, staticData);
        this.groupIndentifierLoader = new PartyGroupIndentifierLoader(em, batchSize, unitdateLoader);
        this.externalIdLoader = new ExternalIdLoader(em, batchSize);
        this.staticData = staticData;
    }

    @Override
    public void flush() {
        super.flush();
        apStateLoader.flush();
        nameLoader.flush();
        groupIndentifierLoader.flush();
        // flush unit-date after name and identifier loader!
        unitdateLoader.flush();
        externalIdLoader.flush();
    }

    @Override
    protected void buildExtendedQuery(Root<? extends ParParty> baseEntity, CriteriaBuilder cb) {
        baseEntity.fetch(ParParty.FIELD_RECORD);
    }

    @Override
    protected PartyInfo createResult(Object entity) {
        ParParty party = (ParParty) entity;
        PartyInfo partyInfo = new PartyInfo(party);

        ApAccessPoint ap = party.getAccessPoint();
        Validate.isTrue(HibernateUtils.isInitialized(ap));

        return partyInfo;
    }

    @Override
    protected void onBatchEntryLoad(LoadDispatcher<PartyInfo> dispatcher, PartyInfo result) {
        ParParty party = result.getParty();

        ApStateDispatcher apsd = new ApStateDispatcher(result, dispatcher, staticData);
        apStateLoader.addRequest(party.getAccessPointId(), apsd);

        NameDispatcher nd = new NameDispatcher(party, dispatcher, staticData);
        nameLoader.addRequest(party.getPartyId(), nd);

        if (party.getPartyType().toEnum() == PartyType.GROUP_PARTY) {
            ParPartyGroup partyGroup = (ParPartyGroup) party;
            PartyGroupIdentifierDispatcher pgid = new PartyGroupIdentifierDispatcher(partyGroup, dispatcher);
            groupIndentifierLoader.addRequest(party.getPartyId(), pgid);
        }

        ExternalIdDispatcher eidd = new ExternalIdDispatcher(result, dispatcher, staticData);
        externalIdLoader.addRequest(party.getAccessPointId(), eidd);
    }

    public static PartyInfoLoader createPartyIdLoader(EntityManager em, int batchSize, StaticDataProvider staticData) {
        return new PartyInfoLoader(ParParty.FIELD_PARTY_ID, em, batchSize, staticData);
    }

    public static PartyInfoLoader createAPIdLoader(EntityManager em, int batchSize, StaticDataProvider staticData) {
        return new PartyInfoLoader(ParParty.FIELD_RECORD_FK, em, batchSize, staticData);
    }
}
