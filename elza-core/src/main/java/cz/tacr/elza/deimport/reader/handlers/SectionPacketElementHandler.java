package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportContext.ImportPhase;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.Packet;

public class SectionPacketElementHandler extends JaxbElementHandler<Packet> {

    public SectionPacketElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    public Class<Packet> getType() {
        return Packet.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<Packet> element) {
        ItemProcessor processor = ItemProcessorFactory.createSectionPacketProcessor(context);
        processor.process(element.getValue());
    }
}
