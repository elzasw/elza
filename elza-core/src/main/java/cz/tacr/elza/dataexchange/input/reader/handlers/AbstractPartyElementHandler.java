package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.AccessPointEntryProcessor;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.Party;

/**
 * Created by todtj on 07.06.2017.
 */
public abstract class AbstractPartyElementHandler<T extends Party> extends JaxbElementHandler<T> {

    protected AbstractPartyElementHandler(ImportContext context) {
        super(context, ImportPhase.PARTIES);
    }

    @Override
    protected final void handleJaxbElement(JAXBElement<T> element) {
        handlePartyApEntry(element.getValue());
        handlePartyElement(element);
    }

    protected abstract void handlePartyElement(JAXBElement<T> element);

    private void handlePartyApEntry(T party) {
        AccessPointEntry entry = party.getApe();
        if (entry == null) {
            throw new DEImportException("Party AccessPointEntry is not set, partyId:" + party.getId());
        }
        try {
            ItemProcessor processor = new AccessPointEntryProcessor(context, true);
            processor.process(entry);
        } catch (DEImportException e) {
            throw new DEImportException("Party AccessPointEntry cannot be processed, partyId:" + party.getId(), e);
        }
    }
}
