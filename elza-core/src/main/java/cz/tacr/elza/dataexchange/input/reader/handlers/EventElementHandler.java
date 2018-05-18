package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.parties.PartyProcessor;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.schema.v2.Event;

public class EventElementHandler extends AbstractPartyElementHandler<Event> {

    public EventElementHandler(ImportContext context) {
        super(context);
    }

    @Override
    protected void handlePartyElement(JAXBElement<Event> element) {
        ItemProcessor processor = new PartyProcessor<Event, ParEvent>(context, ParEvent.class);
        processor.process(element);
    }

    @Override
    public Class<Event> getType() {
        return Event.class;
    }
}
