package cz.tacr.elza.dataexchange.input.reader.handlers;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;

/**
 * Element handler with default JAXB based deserialization.
 * 
 * This handler automatically deserialize input element. Method
 * handleJaxbElement have to be overridden to process this element.
 */
public abstract class JaxbElementHandler<T> extends ContextAwareElementHandler {

    private JAXBContext jaxbContext;

    protected JaxbElementHandler(ImportContext context, ImportPhase phase) {
        super(context, phase);
    }

    @Override
    public final void handleStart(XMLEventReader eventReader, StartElement startElement) {
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

    protected Unmarshaller createUnmarshaller() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(getType());
        }
        return jaxbContext.createUnmarshaller();
    }

    protected JAXBElement<T> unmarshalElement(XMLEventReader eventReader) throws JAXBException {
        Unmarshaller unmarshaller = createUnmarshaller();
        return unmarshaller.unmarshal(eventReader, getType());
    }

	/**
	 * Return target class for deserialization
	 * 
	 * @return
	 */
	public abstract Class<T> getType();

	/**
	 * Method to process deserialized element
	 * 
	 * @param element
	 */
    protected abstract void handleJaxbElement(JAXBElement<T> element);
}
