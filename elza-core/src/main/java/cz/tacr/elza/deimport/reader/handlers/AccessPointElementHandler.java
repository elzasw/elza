package cz.tacr.elza.deimport.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportPhase;
import cz.tacr.elza.deimport.processor.ItemProcessor;
import cz.tacr.elza.deimport.processor.ItemProcessorFactory;
import cz.tacr.elza.schema.v2.AccessPoint;

/**
 * Created by todtj on 24.05.2017.
 */
public class AccessPointElementHandler extends JaxbElementHandler<AccessPoint> {

    public AccessPointElementHandler(ImportContext context) {
        super(context, ImportPhase.ACCESS_POINTS);
    }

    @Override
    public Class<AccessPoint> getType() {
        return AccessPoint.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<AccessPoint> element) {
        ItemProcessor processor = ItemProcessorFactory.createAccessPointProcessor(context);
        processor.process(element.getValue());
    }
}
