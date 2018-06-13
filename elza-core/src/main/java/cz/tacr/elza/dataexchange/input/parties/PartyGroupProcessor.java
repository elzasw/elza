package cz.tacr.elza.dataexchange.input.parties;

import javax.xml.bind.JAXBElement;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointInfo;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.context.PartyGroupIdentifierWrapper;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyGroupIdentifier;
import cz.tacr.elza.schema.v2.PartyGroup;
import cz.tacr.elza.schema.v2.PartyIdentifier;
import cz.tacr.elza.schema.v2.PartyIdentifiers;

public class PartyGroupProcessor extends PartyProcessor<PartyGroup, ParPartyGroup> {

    public PartyGroupProcessor(ImportContext context) {
        super(context, ParPartyGroup.class);
    }

    @Override
    protected void processInternal(JAXBElement<PartyGroup> element) {
        super.processInternal(element);
        processIdentifiers(party.getPis());
    }

    @Override
    protected ParPartyGroup createParty(PartyGroup party, PartyType type, AccessPointInfo apInfo) {
        ParPartyGroup entity = super.createParty(party, type, apInfo);
        entity.setScope(party.getScp());
        entity.setScopeNorm(party.getSn());
        entity.setFoundingNorm(party.getFn());
        entity.setOrganization(party.getStr());
        return entity;
    }

    private void processIdentifiers(PartyIdentifiers identifiers) {
        if (identifiers == null || identifiers.getPi().isEmpty()) {
            return;
        }
        for (PartyIdentifier identifier : identifiers.getPi()) {
            if (StringUtils.isBlank(identifier.getV())) {
                throw new DEImportException("Value of party group indentifier is not set, partyId=" + party.getId());
            }
            ParPartyGroupIdentifier entity = new ParPartyGroupIdentifier();
            entity.setIdentifier(identifier.getV());
            entity.setNote(identifier.getNote());
            entity.setSource(identifier.getSrc());
            PartyGroupIdentifierWrapper wrapper = partiesContext.addIdentifier(entity, info);

            wrapper.setValidFrom(processTimeInterval(identifier.getVf()));
            wrapper.setValidTo(processTimeInterval(identifier.getVto()));
        }
    }
}
