package cz.tacr.elza.dataexchange.input.reader.handlers;

import jakarta.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.AccessPointEntryProcessor;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.parts.PartProcessor;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Party;
import cz.tacr.elza.schema.v2.PartyGroup;

public class PartElementHandler extends JaxbElementHandler<PartyGroup> {

	public PartElementHandler(ImportContext context) {
		super(context, ImportPhase.PARTS);
	}

	@Override
	public Class<PartyGroup> getType() {
		return PartyGroup.class;
	}

    @Override
    protected void handleJaxbElement(JAXBElement<PartyGroup> element) {
        handlePartApEntry(element.getValue());
        ItemProcessor processor = new PartProcessor(context, ApPart.class);
        processor.process(element.getValue());
    }

    private void handlePartApEntry(Party party) {

        AccessPointEntry entry = party.getApe();
        if (entry == null) {
            throw new DEImportException("Party AccessPointEntry is not set, partyId:" + party.getId());
        }
        try {
            ItemProcessor processor = new AccessPointEntryProcessor(context, true);
            processor.process(party);
        } catch (DEImportException e) {
            throw new DEImportException("Party AccessPointEntry cannot be processed, partyId:" + party.getId(), e);
        }
    }
}
