package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class JaxbNode implements XmlNode {

    private final JAXBElement<?> jaxbElement;

    private JAXBContext jaxbContext;

    public JaxbNode(JAXBElement<?> jaxbElement) {
        this.jaxbElement = jaxbElement;
    }

    @Override
    public void write(XMLStreamWriter streamWriter) throws XMLStreamException {
        try {
            Marshaller marshaller = getJAXBContext().createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(jaxbElement, streamWriter);
        } catch (JAXBException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void clear() {
        jaxbContext = null;
    }

    private JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            Class<?> declType = jaxbElement.getDeclaredType();
            jaxbContext = JAXBContext.newInstance(declType);
        }
        return jaxbContext;
    }
}
