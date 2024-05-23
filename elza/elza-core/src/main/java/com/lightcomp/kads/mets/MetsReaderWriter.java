package com.lightcomp.kads.mets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.Marshaller;

import javax.xml.transform.stream.StreamSource;

import com.lightcomp.kads.common.AnyUriAdapter;

import gov.loc.mets.v1_11.schema.Mets;

public class MetsReaderWriter {

    public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(Mets.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }        
    }

    public static Mets unmarshal(InputStream is) throws JAXBException {
        Unmarshaller unm = JAXB_CONTEXT.createUnmarshaller();
        AnyUriAdapter.register(unm, AnyUriAdapter.isLegacyDefault());
        return unm.unmarshal(new StreamSource(is), Mets.class).getValue();
    }

    public static Mets unmarshal(Path path) throws JAXBException, IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return unmarshal(is);
        }
    }

    public static void marshal(Mets mets, Path path) throws JAXBException {
        Marshaller m = JAXB_CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        AnyUriAdapter.register(m, AnyUriAdapter.isLegacyDefault());
        m.marshal(mets, path.toFile());
    }

}
