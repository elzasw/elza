package cz.tacr.elza.deimport.parties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.PartyTypeComplementTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.deimport.parties.context.PartyAccessPointWrapper;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Builds party access points and updates current access points entry.
 */
public class PartiesAccessPointsBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PartiesAccessPointsBuilder.class);

    private final Session session;

    private final StaticDataProvider staticData;

    private final GroovyScriptService groovyScriptService;

    public PartiesAccessPointsBuilder(Session session, StaticDataProvider staticData, GroovyScriptService groovyScriptService) {
        this.session = session;
        this.staticData = staticData;
        this.groovyScriptService = groovyScriptService;
    }

    public List<PartyAccessPointWrapper> build(Collection<PartyImportInfo> partiesInfo) {
        LOG.info("Starting party AP builder.");

        List<PartyAccessPointWrapper> results = new ArrayList<>(partiesInfo.size());
        for (PartyImportInfo info : partiesInfo) {
            RegRecord partyRecord = createPartyRecord(info);
            PartyAccessPointWrapper wrapper = createPartyAccessPoint(info, partyRecord);
            results.add(wrapper);
        }

        LOG.info("Party AP builder finished.");
        return results;
    }

    private RegRecord createPartyRecord(PartyImportInfo info) {
        // supported complement types
        String partyTypeCode = info.getPartyType().getCode();
        PartyTypeComplementTypes partyTypes = staticData.getComplementTypesByPartyTypeCode(partyTypeCode);
        // evaluate groovy script
        ParParty partyRef = info.getEntityRef(session, ParParty.class);
        return groovyScriptService.getRecordFromGroovy(partyRef, partyTypes.getComplementTypes());
    }

    private PartyAccessPointWrapper createPartyAccessPoint(PartyImportInfo info, RegRecord partyRecord) {
        String name = Validate.notEmpty(partyRecord.getRecord());
        // update fulltext index
        info.setFulltext(name);
        // create party access point
        return new PartyAccessPointWrapper(info, name, partyRecord.getCharacteristics(), partyRecord.getNote());
    }
}
