package com.lightcomp.kads.mets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.Marshaller;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import com.lightcomp.kads.common.AnyUriAdapter;

import gov.loc.mets.v1_11.schema.Mets;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

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
        String schemaLocation = " http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd http://www.w3.org/1999/xlink http://www.loc.gov/standards/mets/xlink.xsd https://DILCIS.eu/XML/METS/CSIPExtensionMETS https://earkcsip.dilcis.eu/schema/DILCISExtensionMETS.xsd";
        m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, schemaLocation);
        mets.getOtherAttributes().put(new QName("https://DILCIS.eu/XML/METS/CSIPExtensionMETS", "csip"), "https://DILCIS.eu/XML/METS/CSIPExtensionMETS");
        AnyUriAdapter.register(m, AnyUriAdapter.isLegacyDefault());
        m.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new CustomNamespacePrefixMapper());
        //m.setProperty("com.sun.xml.bind.defaultNamespaceRemap", "http://www.loc.gov/METS/");
        //AnyUriAdapter.register(m, AnyUriAdapter.isLegacyDefault());
        m.marshal(mets, path.toFile());
        // xmlns:csip="https://DILCIS.eu/XML/METS/CSIPExtensionMETS"
    }

    private static class CustomNamespacePrefixMapper extends NamespacePrefixMapper {
        public static final Map<String, String> NAMESPACE_MAP = Map.of(
                "http://www.loc.gov/METS/", "",
                "https://DILCIS.eu/XML/METS/CSIPExtensionMETS", "csip",
                "http://www.w3.org/2001/XMLSchema-instance", "xsi",
                "http://www.w3.org/1999/xlink", "xlink"
        );

        private Map<String, String> namespaceMap;
        public CustomNamespacePrefixMapper(final Map<String, String> namespaceMap) {
            this.namespaceMap = namespaceMap;
        }
        public CustomNamespacePrefixMapper() {
            this(new HashMap<>(NAMESPACE_MAP));
        }
        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            return namespaceMap.getOrDefault(namespaceUri, suggestion);
        }
    }

}
