package cz.tacr.elza.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
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

    public static LocalDate convertToLocalDate(XMLGregorianCalendar v) {
        if (v == null) {
            return null;
        }
        TimeZone tz = v.getTimezone() == DatatypeConstants.FIELD_UNDEFINED ? UTC_TIMEZONE : null;
        GregorianCalendar gc = v.toGregorianCalendar(tz, null, null);
        return gc.toZonedDateTime().toLocalDate();
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

    public static XMLGregorianCalendar convertDate(Date localDate) {
        if (localDate == null) {
            return null;
        }
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTime(localDate);
        return DATATYPE_FACTORY.newXMLGregorianCalendar(gcal.get(Calendar.YEAR),
                                                        gcal.get(Calendar.MONTH) + 1, // měsíce začínají na 0, ne 1
                                                        gcal.get(Calendar.DAY_OF_MONTH),
                                                        DatatypeConstants.FIELD_UNDEFINED,
                                                        DatatypeConstants.FIELD_UNDEFINED,
                                                        DatatypeConstants.FIELD_UNDEFINED,
                                                        DatatypeConstants.FIELD_UNDEFINED,
                                                        DatatypeConstants.FIELD_UNDEFINED);
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
        return DATATYPE_FACTORY.newXMLGregorianCalendar(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute(),
                dateTime.getSecond(),
                dateTime.getNano() / 1000000,
                DatatypeConstants.FIELD_UNDEFINED);
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
    public static <T> JAXBElement<T> wrapElement(String localName, T element) {
        return wrapElement(new QName(localName), element);
    }

    /**
     * Wraps element to JAXB element. Commonly used for fragment serialization of XML type.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> JAXBElement<T> wrapElement(QName name, T element) {
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

    /**
     * Převede data z objektu do xml.
     *
     * @param data
     *            data
     *
     * @return pole bytů
     */
    public static <T, C> byte[] marshallData(final T data, final Class<C> cls) {
        Validate.notNull(data);

        try {
            Marshaller marshaller = createMarshaller(cls);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshaller.marshal(data, outputStream);

            return outputStream.toByteArray();
        } catch (JAXBException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Vytvoří marshaller pro data.
     *
     * @return marshaller pro data
     */
    private static <C> Marshaller createMarshaller(final Class<C> cls) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(cls);
        Marshaller marshaller = jaxbContext.createMarshaller();

        return marshaller;
    }

    /**
     * Naformátuje předané xml.
     *
     * @param inputStream
     *            xml
     *
     * @return naformátované xml
     */
    public static String formatXml(final InputStream inputStream) {
        try {
            TransformerFactory transFactory = TransformerFactory.newInstance();
            transFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());

            StreamSource ss = new StreamSource(inputStream);

            transformer.transform(ss, result);
            return result.getWriter().toString();
        } catch (TransformerException e) {
            throw new SystemException("Chyba při formátování xml.", e);
        }
    }

    /**
     * Cannot instantiate class
     */
    private XmlUtils() {

    }

}
