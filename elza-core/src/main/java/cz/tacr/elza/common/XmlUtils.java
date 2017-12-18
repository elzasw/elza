package cz.tacr.elza.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.Validate;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import cz.tacr.elza.exception.SystemException;

/**
 * Helper class for XML operations.
 */
public class XmlUtils {

    /**
     * Default data-type factory. Initialized factory is considered to be thread-safe.
     */
    public static final DatatypeFactory DATATYPE_FACTORY = XmlUtils.createDatatypeFactory();

    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC");

    public static final String XSLT_EXTENSION = ".xslt";

    /**
     * Unmarshal xml element as declared type from string.
     */
    public static <T> T unmarshall(String xmlValue, Class<T> declaredType) {
        try (StringReader reader = new StringReader(xmlValue)) {
            return unmarshall(new StreamSource(reader), declaredType);
        }
    }

    /**
     * Unmarshal xml element as declared type from stream source.
     */
    public static <T> T unmarshall(StreamSource xmlSource, Class<T> declaredType) {
        if (xmlSource == null) {
            return null;
        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(declaredType);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<T> element = unmarshaller.unmarshal(xmlSource, declaredType);
            return element.getValue();
        } catch (JAXBException e) {
            throw new SystemException("Failed to unmarshall xml element", e);
        }
    }

    /**
     * Validate XML input stream by XSD schema.
     *
     * @param is xml input stream, not-null
     * @param xsdSchema URL to resource of schema, not-null
     * @param handler custom error handling, can be null
     * @throws SAXException If the ErrorHandler throws a SAXException or if a fatal error is found
     *             and the ErrorHandler returns normally.
     */
    public static void validateXml(InputStream is, URL xsdSchema, ErrorHandler handler) throws SAXException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // open stream reader and load schema
        XMLStreamReader streamReader = null;
        Schema schema = null;
        try {
            streamReader = inputFactory.createXMLStreamReader(is);
            schema = schemaFactory.newSchema(xsdSchema);
        } catch (Exception e) {
            throw new SystemException(e);
        }
        // create validator
        Validator validator = schema.newValidator();
        if (handler != null) {
            validator.setErrorHandler(handler);
        }
        // validate xml
        try {
            validator.validate(new StAXSource(streamReader), null);
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Converts {@link XMLGregorianCalendar} to {@link LocalDateTime}.<br>
     * Xml calendar with undefined timezone is converted without time shift.<br>
     * Any date before October 15, 1582 will be recalculate from Julian to Gregorian calendar.
     *
     * @param calendar xml date-time, can be null
     * @return {@link LocalDateTime} or null when calendar was null.
     */
    public static LocalDateTime convertXmlDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        TimeZone tz = calendar.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? UTC_TIMEZONE : null;
        GregorianCalendar gc = calendar.toGregorianCalendar(tz, null, null);
        Instant instant = Instant.ofEpochMilli(gc.getTimeInMillis());
        return LocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), ZoneOffset.UTC);
    }

    /**
     * Converts {@link LocalDateTime} to {@link XMLGregorianCalendar}.<br>
     * Possible precision loss (only milliseconds are converted).
     *
     * @param dateTime can be null
     * @param dtf not-null
     * @return {@link XMLGregorianCalendar} or null when dateTime was null.
     */
    public static XMLGregorianCalendar convertDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        XMLGregorianCalendar xmlGc = DATATYPE_FACTORY.newXMLGregorianCalendar(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond(),
                dateTime.getNano() / 1000000,
                DatatypeConstants.FIELD_UNDEFINED);
        return xmlGc;
    }

    /**
     * Helper method for creating new JAXB context instance.
     */
    public static JAXBContext createJAXBContext(Class<?>... jaxbClasses) {
        Validate.notEmpty(jaxbClasses);
        try {
            return JAXBContext.newInstance(jaxbClasses);
        } catch (JAXBException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Wraps element to JAXB element. Commonly used for fragment serialization of XML type.
     */
    public static JAXBElement<?> wrapElement(String localName, Object element) {
        return wrapElement(new QName(localName), element);
    }

    /**
     * Wraps element to JAXB element. Commonly used for fragment serialization of XML type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static JAXBElement<?> wrapElement(QName name, Object element) {
        Validate.notNull(name);
        return new JAXBElement(name, element.getClass(), element);
    }

    /**
     * Helper method for creating new DatatypeFactory instance.
     */
    public static DatatypeFactory createDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new SystemException(e);
        }
    }
}
