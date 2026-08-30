package com.lightcomp.kads.premis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.Marshaller;

import javax.xml.namespace.QName;

import com.lightcomp.kads.common.XmlRootReader;

import gov.loc.premis.v3.PremisComplexType;

public class PremisReaderWriter {

    /** Root element of a PREMIS v3 document. */
    public static final QName ROOT_ELEMENT = new QName("http://www.loc.gov/premis/v3", "premis");

    public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(PremisComplexType.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static PremisComplexType unmarshal(InputStream is) throws JAXBException {
        Unmarshaller unm = JAXB_CONTEXT.createUnmarshaller();
        return XmlRootReader.unmarshal(is, unm, PremisComplexType.class, ROOT_ELEMENT);
    }

    public static PremisComplexType unmarshal(Path path) throws JAXBException, IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return unmarshal(is);
        }
    }

    public static void marshal(PremisComplexType mets, Path path) throws JAXBException {
        Marshaller m = JAXB_CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(mets, path.toFile());
    }

}
