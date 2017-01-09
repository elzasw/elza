package cz.tacr.elza.dao.common;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import cz.tacr.elza.dao.exception.DaoComponentException;

public class XmlUtils {

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void marshalXmlType(Class<?> type, Object element, OutputStream os) {
		JAXBElement<Class<?>> root = new JAXBElement(new QName(type.getSimpleName()), type, element);
		marshalXmlRoot(type, root, os);
	}

	public static void marshalXmlRoot(Class<?> type, Object element, OutputStream os) {
		Marshaller marshaller;
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(type);
			marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
			marshaller.marshal(element, os);
		} catch (final JAXBException e) {
			throw new DaoComponentException("cannot marshal xml object", e);
		}
	}
}