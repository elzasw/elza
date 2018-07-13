package cz.tacr.elza.dataexchange.input.parties;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.schema.v2.Family;

public class FamilyProcessor extends PartyProcessor<Family, ParDynasty> {

    public FamilyProcessor(ImportContext context) {
        super(context, ParDynasty.class);
    }

    @Override
    protected ParDynasty createParty(Family party, PartyType type, AccessPointInfo apInfo) {
        ParDynasty entity = super.createParty(party, type, apInfo);
        entity.setGenealogy(party.getGen());
        return entity;
    }
}
