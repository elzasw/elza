package cz.tacr.elza.connector;


import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;
import org.springframework.core.io.InputStreamSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public abstract class JaxbUtils {

    public static <T> T unmarshal(final Class<T> classObject, final InputStreamSource resource) {
        try (InputStream in = resource.getInputStream()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(in);
        } catch (Exception e) {
            throw new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", classObject.toString());
        }
    }

    public static <T> T unmarshal(final Class<T> classObject, final InputStream inputStream) {
        try (InputStream in = inputStream) {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(in);
        } catch (Exception e) {
            throw new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", classObject.toString());
        }
    }

    public static <T> T unmarshal(final Class<T> classObject, final File file) {
        try (InputStream in = new FileInputStream(file)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(classObject);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (T) unmarshaller.unmarshal(in);
        } catch (Exception e) {
            throw new SystemException("Nepodařilo se načíst objekt " + classObject.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", classObject.toString());
        }
    }


    public static <T> File asFile(final T body) {
        Class<?> aClass = body.getClass();
        try {
            File temp = File.createTempFile("cam-", ".api.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(aClass);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            try (FileOutputStream os = new FileOutputStream(temp);
                 OutputStreamWriter wf = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                jaxbMarshaller.marshal(body, wf);
            }
            return temp;
        } catch (Exception e) {
            throw new SystemException("Nepodařilo se načíst objekt " + aClass.getSimpleName() + " ze streamu", e, PackageCode.PARSE_ERROR).set("class", aClass.toString());
        }
    }
}
