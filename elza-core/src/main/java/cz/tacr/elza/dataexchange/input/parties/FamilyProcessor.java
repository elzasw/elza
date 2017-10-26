package cz.tacr.elza.dataexchange.input.parties;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.schema.v2.Family;

public class FamilyProcessor extends PartyProcessor<Family, ParDynasty> {

    public FamilyProcessor(ImportContext context) {
        super(context, ParDynasty.class);
    }

    @Override
    protected ParDynasty createEntity(Family item) {
        ParDynasty entity = super.createEntity(item);
        entity.setGenealogy(item.getGen());
        return entity;
    }
}
