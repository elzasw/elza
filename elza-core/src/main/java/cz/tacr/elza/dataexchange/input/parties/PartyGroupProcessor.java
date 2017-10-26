package cz.tacr.elza.dataexchange.input.parties;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyGroupIdentifierWrapper;
import cz.tacr.elza.dataexchange.input.parties.context.PartyInfo;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.schema.v2.PartyGroup;
import cz.tacr.elza.schema.v2.PartyIdentifier;

public class PartyGroupProcessor extends PartyProcessor<PartyGroup, ParPartyGroup> {

    public PartyGroupProcessor(ImportContext context) {
        super(context, ParPartyGroup.class);
    }

    @Override
    protected ParPartyGroup createEntity(PartyGroup item) {
        ParPartyGroup entity = super.createEntity(item);
        entity.setScope(item.getScp());
        entity.setScopeNorm(item.getSn());
        entity.setFoundingNorm(item.getFn());
        entity.setOrganization(item.getStr());
        return entity;
    }

    @Override
    protected void processSubEntities(PartyGroup item, PartyInfo partyInfo) {
        super.processSubEntities(item, partyInfo);
        processPartyGroupIdentifiers(item, partyInfo);
    }

    private void processPartyGroupIdentifiers(PartyGroup item, PartyInfo partyInfo) {
        if (item.getPis() != null) {
            for (PartyIdentifier identifier : item.getPis().getPi()) {
                if (StringUtils.isBlank(identifier.getV())) {
                    throw new DEImportException("Value of party group indentifier is not set, partyId:" + item.getId());
                }
                ParPartyGroupIdentifier entity = new ParPartyGroupIdentifier();
                entity.setIdentifier(identifier.getV());
                entity.setNote(identifier.getNote());
                entity.setSource(identifier.getSrc());
                PartyGroupIdentifierWrapper info = partiesContext.addGroupIdentifier(entity, partyInfo);
                info.setValidFrom(processTimeInterval(identifier.getVf(), partyInfo));
                info.setValidTo(processTimeInterval(identifier.getVto(), partyInfo));
            }
        }
    }
}
