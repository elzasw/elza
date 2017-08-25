package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportPhase;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
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
        handlePartyAccessPointEntry(element.getValue());
        handlePartyElement(element);
    }

    protected abstract void handlePartyElement(JAXBElement<T> element);

    private void handlePartyAccessPointEntry(T party) {
        AccessPointEntry entry = party.getApe();
        if (entry == null) {
            throw new DEImportException("Party AccessPointEntry is not set, partyId:" + party.getId());
        }
        try {
            ItemProcessor processor = ItemProcessorFactory.createPartyAccessPointEntryProcessor(context);
            processor.process(entry);
        } catch (DEImportException e) {
            throw new DEImportException("Party AccessPointEntry cannot be processed, partyId:" + party.getId(), e);
        }
    }
}
