package cz.tacr.elza.dataexchange.input.parties.aps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.core.data.PartyTypeComplementTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.service.GroovyScriptService;

/**
 * Builds party access points and updates current access points entry.
 */
public class PartiesAccessPointsBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PartiesAccessPointsBuilder.class);

    private final StaticDataProvider staticData;

    private final GroovyScriptService groovyScriptService;

    private final Session session;

    public PartiesAccessPointsBuilder(StaticDataProvider staticData, GroovyScriptService groovyScriptService, Session session) {
        this.staticData = staticData;
        this.groovyScriptService = groovyScriptService;
        this.session = session;
    }

    public List<PartyAccessPointWrapper> build(Collection<PartyInfo> partiesInfo) {
        LOG.info("Starting party AP builder.");

        List<PartyAccessPointWrapper> results = new ArrayList<>(partiesInfo.size());
        for (PartyInfo info : partiesInfo) {
            if (info.isIgnored()) {
                continue;
            }
            RegRecord partyRecord = createPartyRecord(info);
            PartyAccessPointWrapper wrapper = createPartyAccessPoint(info, partyRecord);
            results.add(wrapper);
        }

        LOG.info("Party AP builder finished.");
        return results;
    }

    private RegRecord createPartyRecord(PartyInfo info) {
        // supported complement types
        String partyTypeCode = info.getPartyType().getCode();
        PartyTypeComplementTypes partyTypes = staticData.getComplementTypesByPartyTypeCode(partyTypeCode);
        // evaluate groovy script
        ParParty party = info.getEntityReference(session);
        return groovyScriptService.getRecordFromGroovy(party, partyTypes.getComplementTypes());
    }

    private PartyAccessPointWrapper createPartyAccessPoint(PartyInfo partyInfo, RegRecord ap) {
        String name = Validate.notEmpty(ap.getRecord());
        // update fulltext index
        partyInfo.setAPName(name);
        // create party access point
        return new PartyAccessPointWrapper(partyInfo, name, ap.getCharacteristics(), ap.getNote());
    }
}