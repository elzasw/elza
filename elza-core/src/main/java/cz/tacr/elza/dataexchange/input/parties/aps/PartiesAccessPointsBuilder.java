package cz.tacr.elza.dataexchange.input.parties.aps;

import cz.tacr.elza.core.data.PartyTypeComplementTypes;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyScriptService;
import cz.tacr.elza.service.vo.ApAccessPointData;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds party access points and updates current access points entry.
 */
public class PartiesAccessPointsBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PartiesAccessPointsBuilder.class);

    private final StaticDataProvider staticData;

    private final GroovyScriptService groovyScriptService;

    private final Session session;

    private final AccessPointDataService accessPointDataService;

    public PartiesAccessPointsBuilder(StaticDataProvider staticData, GroovyScriptService groovyScriptService, Session session, AccessPointDataService accessPointDataService) {
        this.staticData = staticData;
        this.groovyScriptService = groovyScriptService;
        this.session = session;
        this.accessPointDataService = accessPointDataService;
    }

    public List<PartyAccessPointWrapper> build(Collection<PartyInfo> partiesInfo) {
        LOG.info("Starting party AP builder.");

        List<PartyAccessPointWrapper> results = new ArrayList<>(partiesInfo.size());

        List<Integer> apIds = partiesInfo.stream().map(PartyInfo::getAPId).collect(Collectors.toList());
        Map<Integer, ApAccessPointData> pointDataMap = accessPointDataService.mapAccessPointDataById(apIds);

        for (PartyInfo info : partiesInfo) {
            if (info.isIgnored()) {
                continue;
            }
            ApAccessPoint partyRecord = createPartyRecord(info);
            PartyAccessPointWrapper wrapper = createPartyAccessPoint(info, partyRecord, pointDataMap);
            results.add(wrapper);
        }

        LOG.info("Party AP builder finished.");
        return results;
    }

    private ApAccessPoint createPartyRecord(PartyInfo info) {
        // supported complement types
        String partyTypeCode = info.getPartyType().getCode();
        PartyTypeComplementTypes partyTypes = staticData.getComplementTypesByPartyTypeCode(partyTypeCode);
        // evaluate groovy script
        ParParty party = info.getEntityReference(session);
        return groovyScriptService.getRecordFromGroovy(party, partyTypes.getComplementTypes());
    }

    private PartyAccessPointWrapper createPartyAccessPoint(PartyInfo partyInfo, ApAccessPoint ap, Map<Integer, ApAccessPointData> pointDataMap) {
        ApAccessPointData pointData = pointDataMap.get(ap.getAccessPointId());
        String name = Validate.notEmpty(pointData.getPreferredName().getName());
        // update fulltext index
        partyInfo.setAPName(name);
        // create party access point
        return new PartyAccessPointWrapper(partyInfo, name, pointData.getDescription().getDescription());
    }
}
