package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;

/**
 * Created by todtj on 11.05.2017.
 */
public abstract class JaxbElementHandler<T> extends ContextAwareElementHandler {

    private JAXBContext jaxbContext;

    protected JaxbElementHandler(ImportContext context, ImportPhase phase) {
        super(context, phase);
    }

    @Override
    public final void handleStart(XMLEventReader eventReader) {
        JAXBElement<T> jaxbElement;
        try {
            jaxbElement = unmarshalElement(eventReader);
        } catch (JAXBException e) {
            throw new DEImportException("Cannot deserialize " + getType(), e);
        }
        handleJaxbElement(jaxbElement);
    }

    @Override
    public final void handleEnd() {
        // JAXB unmarshaller consumes end element
    }

    public abstract Class<T> getType();

    protected Unmarshaller createUnmarshaller(JAXBContext jaxbContext) throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(getType());
        }
        return jaxbContext.createUnmarshaller();
    }

    protected JAXBElement<T> unmarshalElement(XMLEventReader eventReader) throws JAXBException {
        Unmarshaller unmarshaller = createUnmarshaller(jaxbContext);
        return unmarshaller.unmarshal(eventReader, getType());
    }

    protected abstract void handleJaxbElement(JAXBElement<T> element);
}
