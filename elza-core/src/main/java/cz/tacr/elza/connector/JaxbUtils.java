package cz.tacr.elza.connector;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamSource;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.PackageCode;

/**
 *
 */
public abstract class JaxbUtils {

    private static Logger log = LoggerFactory.getLogger(JaxbUtils.class);

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

    public static <T> File asFile(final T body, Schema schema) {
        Class<?> aClass = body.getClass();
        try {
            File temp = File.createTempFile("cam-", ".api.xml");
            JAXBContext jaxbContext = JAXBContext.newInstance(aClass);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setSchema(schema);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            try (FileOutputStream os = new FileOutputStream(temp);
                 OutputStreamWriter wf = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                jaxbMarshaller.marshal(body, wf);
            }
            return temp;
        } catch (Exception e) {
            log.error("Failed to write XML, class: " + aClass.getSimpleName(), e);
            throw new SystemException("Nepodařilo se uložit objekt " + aClass.getSimpleName() + " do souboru", e,
                    BaseCode.EXPORT_FAILED).set("class", aClass.toString());
        }
    }
}
