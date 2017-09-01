package cz.tacr.elza.deimport.parties;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.parties.context.PartyGroupIdentifierWrapper;
import cz.tacr.elza.deimport.parties.context.PartyImportInfo;
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
    protected void processSubEntities(PartyGroup item, PartyImportInfo partyInfo) {
        super.processSubEntities(item, partyInfo);
        processPartyGroupIdentifiers(item, partyInfo);
    }

    private void processPartyGroupIdentifiers(PartyGroup item, PartyImportInfo partyInfo) {
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
                String partyImportId = partyInfo.getImportId();
                info.setFrom(processTimeInterval(identifier.getVf(), partyImportId));
                info.setTo(processTimeInterval(identifier.getVto(), partyImportId));
            }
        }
    }
}
