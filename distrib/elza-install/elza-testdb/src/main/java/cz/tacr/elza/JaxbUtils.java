/**
 *
 */
package cz.tacr.elza;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Podpora pro praci s Jaxb pro ukladani a nacitani objektu do XML.
 *
 */
public class JaxbUtils {


    public static <T> void save(final T objekt, final String file) {
        try {
            JAXBContext context = JAXBContext.newInstance(objekt.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(objekt, new File(file));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }

    public static <T> T load(final String file, Class<T> clazz) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            T result = (T) unmarshaller.unmarshal(new File(file));
            return result;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
