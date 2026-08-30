package com.lightcomp.kads.common;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Unmarshals a document whose root element is known in advance.
 *
 * {@link Unmarshaller#unmarshal(javax.xml.transform.Source, Class)} maps the document onto the
 * declared type whatever its root element is, so a document of a different standard is not
 * rejected - it is silently mapped onto an object with all properties left empty, and the
 * defect only surfaces much later as a missing value. This reader checks the root element
 * first and rejects such a document with a description of what it actually contains.
 */
public class XmlRootReader {

    private static final XMLInputFactory INPUT_FACTORY = createInputFactory();

    /**
     * Reads the document as an instance of the given type.
     *
     * @param unmarshaller unmarshaller of the target standard, already configured by the caller
     * @param expectedRoot root element the document has to start with
     * @throws XmlContentException the document is not well-formed, is empty or starts with a
     *             different root element
     */
    public static <T> T unmarshal(InputStream is, Unmarshaller unmarshaller, Class<T> type, QName expectedRoot)
            throws JAXBException {
        XMLStreamReader reader = null;
        try {
            reader = INPUT_FACTORY.createXMLStreamReader(is);
            while (reader.hasNext() && reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
                reader.next();
            }
            if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
                throw new XmlContentException("Soubor neobsahuje žádný XML element, očekáván kořenový element "
                        + describe(expectedRoot) + ".");
            }
            QName rootElement = reader.getName();
            if (!expectedRoot.equals(rootElement)) {
                throw new XmlContentException("Očekáván kořenový element " + describe(expectedRoot)
                        + ", soubor má kořenový element " + describe(rootElement) + ".");
            }
            return unmarshaller.unmarshal(reader, type).getValue();
        } catch (XMLStreamException e) {
            throw new XmlContentException("Soubor není platný XML dokument: " + e.getMessage(), e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Names the element the way it is written in the document - the local name with the
     * namespace it belongs to, because the namespace is what tells the standards apart.
     */
    private static String describe(QName element) {
        String namespace = element.getNamespaceURI();
        return "<" + element.getLocalPart() + ">"
                + (namespace == null || namespace.isEmpty()
                        ? " bez jmenného prostoru"
                        : " ve jmenném prostoru '" + namespace + "'");
    }

    private static void closeQuietly(XMLStreamReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                // the underlying stream is owned and closed by the caller
            }
        }
    }

    private static XMLInputFactory createInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return factory;
    }
}
