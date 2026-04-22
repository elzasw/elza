package cz.tacr.elza.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.archivists.ead3.schema.Ead;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class EadReaderWriter {

	public static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(Ead.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static Ead unmarshal(InputStream is) throws JAXBException {
        Unmarshaller unm = JAXB_CONTEXT.createUnmarshaller();
        return unm.unmarshal(new StreamSource(is), Ead.class).getValue();
    }

    public static Ead unmarshal(Path path) throws JAXBException, IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return unmarshal(is);
        }
    }

    public static void marshal(Ead mets, Path path) throws JAXBException {
        Marshaller m = JAXB_CONTEXT.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(mets, path.toFile());
    }

}
