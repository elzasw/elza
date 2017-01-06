package cz.tacr.elza.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.GregorianCalendar;

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
import javax.xml.transform.stream.StreamSource;

import cz.tacr.elza.dao.exception.DaoComponentException;

public class XmlUtils {

	private static final Charset XML_CHARSET = StandardCharsets.UTF_8;

	public static XMLGregorianCalendar convertDate(Date date) {
		if (date == null) {
			return null;
		}
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		try {
			XMLGregorianCalendar xmlgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
			xmlgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
			return xmlgc;
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public static String marshalXml(Class<?> type, Object element, boolean hasXmlRoot) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		if (hasXmlRoot) {
			marshalXmlRoot(type, element, stream);
		} else {
			marshalXmlType(type, element, stream);
		}
		try {
			return stream.toString(XML_CHARSET.name());
		} catch (UnsupportedEncodingException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Expecting XmlType object without XmlRootElement annotation. New root with name
	 * Class&lt;T&gt;.getSimpleName() is created.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void marshalXmlType(Class<?> type, Object element, OutputStream stream) {
		JAXBElement<Class<?>> root = new JAXBElement(new QName(type.getSimpleName()), type, element);
		marshalXmlRoot(type, root, stream);
	}

	public static void marshalXmlRoot(Class<?> type, Object element, OutputStream stream) {
		Marshaller marshaller;
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(type);
			marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, XML_CHARSET.name());
			marshaller.marshal(element, stream);
		} catch (final JAXBException e) {
			throw new DaoComponentException("Cannot marshal XML object " + type.getSimpleName(), e);
		}
	}

	public static <T> T unmarshalXml(Class<T> type, String element, boolean hasXmlRoot) {
		ByteArrayInputStream stream = new ByteArrayInputStream(element.getBytes(XML_CHARSET));
		if (hasXmlRoot) {
			return unmarshalXmlRoot(type, stream);
		} else {
			return unmarshalXmlType(type, stream);
		}
	}

	/**
	 * Expecting XmlType object with temporary root element. Child of root will match &lt;T&gt;.
	 */
	public static <T> T unmarshalXmlType(Class<T> type, InputStream stream) {
		Unmarshaller unmarshaller;
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(type);
			unmarshaller = context.createUnmarshaller();
			return unmarshaller.unmarshal(new StreamSource(stream), type).getValue();
		} catch (final JAXBException e) {
			throw new DaoComponentException("Cannot unmarshal XML object " + type.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T unmarshalXmlRoot(Class<T> type, InputStream stream) {
		Unmarshaller unmarshaller;
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(type);
			unmarshaller = context.createUnmarshaller();
			return (T) unmarshaller.unmarshal(stream);
		} catch (final JAXBException e) {
			throw new DaoComponentException("Cannot unmarshal XML object " + type.getSimpleName(), e);
		}
	}
}