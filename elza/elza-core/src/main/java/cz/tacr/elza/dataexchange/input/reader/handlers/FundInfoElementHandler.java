package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBElement;

import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.schema.v2.FundInfo;

public class FundInfoElementHandler extends JaxbElementHandler<FundInfo> {

    public FundInfoElementHandler(ImportContext context) {
        super(context, ImportPhase.SECTIONS);
    }

    @Override
    public Class<FundInfo> getType() {
        return FundInfo.class;
    }

    @Override
    protected void handleJaxbElement(JAXBElement<FundInfo> element) {
        context.getSections().setFundInfo(element.getValue());
    }
}
