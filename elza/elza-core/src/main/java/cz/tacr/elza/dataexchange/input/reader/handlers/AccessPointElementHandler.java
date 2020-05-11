package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.aps.AccessPointProcessor;
import cz.tacr.elza.dataexchange.input.aps.FragmentsProcessor;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.reader.ItemProcessor;
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
        ItemProcessor processor = new AccessPointProcessor(context);
        processor.process(element.getValue());
        //processor = new FragmentsProcessor(context);

    }
}
