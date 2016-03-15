package cz.tacr.elza.xmlimport;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.api.vo.XmlImportType;
import cz.tacr.elza.service.XmlImportService;
import cz.tacr.elza.service.exception.XmlImportException;
import cz.tacr.elza.xmlimport.v1.utils.XmlImportConfig;
import cz.tacr.elza.xmlimport.v1.vo.XmlImport;

/**
 * Testy na xml import.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
public class XmlImportTest extends AbstractTest implements ApplicationContextAware {

    @Autowired
    private XmlDataGenerator xmlDataGenerator;

    @Autowired
    private XmlImportService xmlImportService;

    private static final int RECORD_COUNT = 10;

    private static final int VARIANT_RECORD_COUNT = 3;

    private static final int PARTY_COUNT = 10;

    private static final int CHILD_COUNT = 3;

    private static final int TREE_DEPTH_COUNT = 2;

    private static final int DESC_ITEMS_COUNT = 12;

    private static final int EVENT_COUNT = 1;

    private static final int PARTY_GROUP_ID_COUNT = 1;

    private static final int PARTY_NAME_COMPLEMENTS_COUNT = 1;

    private static final int PACKET_COUNT = 10;

    private ApplicationContext applicationContext;

    /**
     * Test na import nové pomůcky v nativním formátu.
     */
    //@Test
    public void importNativeData() throws XmlImportException {
        XmlImportConfig config = new XmlImportConfig();
        config.setStopOnError(false);
        config.setXmlImportType(XmlImportType.FUND);
        //        config.setXmlFile(new File("d:\\xml-export1.xml"));

        xmlImportService.importData(config);
    }

    /**
     * Vytvoření xml souboru s osobami a rejstříky.
     */
    //    @Test
    public void testCreatePartyFile() throws IOException, JAXBException {
        XmlDataGeneratorConfig config = new XmlDataGeneratorConfig(3, 1, 4, 0, 0, 0, true, 2, 2, 2, 0);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        File out = File.createTempFile("xml-export-parties", ".xml");

        createMarshaller().marshal(fa, out);

        System.out.println("Cesta k vytvořenému souboru: " + out.getAbsolutePath());
    }

    /**
     * Vytvoření xml souboru s rejstříky.
     */
    @Test
    public void testCreateRecordFile() throws IOException, JAXBException {
        XmlDataGeneratorConfig config = new XmlDataGeneratorConfig(3, 1, 0, 0, 0, 0, true, 0, 0, 0, 0);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        File out = File.createTempFile("xml-export-records", ".xml");

        createMarshaller().marshal(fa, out);

        System.out.println("Cesta k vytvořenému souboru: " + out.getAbsolutePath());
    }

    /**
     * Načtení velkého souboru.
     */
    //    @Test
    public void testLoadLargeXmlFile() throws JAXBException {
        File file = new File(
                "d:\\marbes\\projekty\\elza\\elza-core\\src\\test\\resources\\xml-export-large291375430279915953.xml");

        long start = System.nanoTime();
        XmlImport faFromFile = (XmlImport) createUnmarshaller().unmarshal(file);
        long stop = System.nanoTime();
        System.out.println("hotovo " + (stop - start) / 1000 + " ms");
    }

    /**
     * Vytvoření velkého xml souboru.
     */
    //    @Test
    public void testCreateLargeXmlFile() throws JAXBException, IOException {
        //1,5 GB
        //        XmlDataGeneratorConfig config = new XmlDataGeneratorConfig(50, 10, 50, 50, 15, 10, true, 5, 3, 3, 3);
        //300 MB
        XmlDataGeneratorConfig config = new XmlDataGeneratorConfig(50, 10, 50, 20, 4, 10, true, 5, 3, 3, 3);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        File out = File.createTempFile("xml-export-large", ".xml");

        createMarshaller().marshal(fa, out);

        System.out.println("Cesta k vytvořenému souboru: " + out.getAbsolutePath());
    }

    /**
     * Test na zápis dat do xml, jejich načtení a porovnání.
     */
    @Test
    public void testDataIntegrity() throws JAXBException, IOException {
        XmlDataGeneratorConfig config = createDefaultGeneratorConfig(true);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        File out = File.createTempFile("xml-export", ".xml");
        out.deleteOnExit();

        createMarshaller().marshal(fa, out);

        XmlImport faFromFile = (XmlImport) createUnmarshaller().unmarshal(out);

        Assert.isTrue(fa.equals(faFromFile));
    }

    /**
     * Test na validaci dat oproti vygenerovanému xsd.
     */
    @Test
    public void testValidity() throws JAXBException, SAXException, IOException {
        XmlDataGeneratorConfig config = createDefaultGeneratorConfig(true);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        Marshaller marshaller = createMarshaller();
        marshaller.setSchema(createSchema());
        marshaller.marshal(fa, new DefaultHandler());

        Assert.isTrue(true);
    }

    /**
     * Test na validaci dat oproti vygenerovanému xsd.
     */
    //@Test
    public void testValidityJV() throws JAXBException, SAXException, IOException {
        //        XmlDataGeneratorConfig config = createDefaultGeneratorConfig(true);
        //        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        String path
                = "d:\\marbes\\dokumenty\\elza\\ELZA-228 - MCV MT09 XSD pro rejstříky a osoby (původce) z INTERPI\\testovaci xml\\";
        String[] files = {
                "ELZA+xml-parties-DYNASTY.xml",
                "ELZA+xml-parties-EVENT.xml",
                "ELZA+xml-parties-PARTY_GROUP.xml",
                "ELZA+xml-parties-PERSON.xml",
                "ELZA-xml-records-ARTWORK.xml",
                "ELZA-xml-records-GEO.xml",
                "ELZA-xml-records-TERM.xml"};

        for (String f : files) {
            File file = new File(path + f);

            long start = System.nanoTime();
            XmlImport fa = (XmlImport) createUnmarshaller().unmarshal(file);
            long stop = System.nanoTime();

            Marshaller marshaller = createMarshaller();
            marshaller.setSchema(createSchema());
            marshaller.marshal(fa, new DefaultHandler());
            System.out.println("Soubor " + f);
        }

        Assert.isTrue(true);
    }

    /**
     * Test na validaci dat oproti vygenerovanému xsd. Data nejsou validní.
     */
    @Test(expected = MarshalException.class)
    public void testValidityWithInvalidData() throws JAXBException, SAXException, IOException {
        XmlDataGeneratorConfig config = createDefaultGeneratorConfig(false);
        XmlImport fa = xmlDataGenerator.createXmlImportData(config);

        Marshaller marshaller = createMarshaller();
        marshaller.setSchema(createSchema());
        marshaller.marshal(fa, new DefaultHandler());

        Assert.isTrue(false);
    }

    /**
     * Vytvoření nastavení pro generátor.
     *
     * @param valid příznak zda mají být vygenerovaná data validní
     * @return nastavení pro generátor
     */
    private XmlDataGeneratorConfig createDefaultGeneratorConfig(final boolean valid) {
        return new XmlDataGeneratorConfig(RECORD_COUNT, VARIANT_RECORD_COUNT, PARTY_COUNT, CHILD_COUNT,
                TREE_DEPTH_COUNT,
                DESC_ITEMS_COUNT, valid, EVENT_COUNT, PARTY_GROUP_ID_COUNT,
                PARTY_NAME_COMPLEMENTS_COUNT, PACKET_COUNT);
    }

    /**
     * Vytvoří marshaller pro zápis dat do xml.
     *
     * @return marshaller
     */
    private Marshaller createMarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlImport.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        return marshaller;
    }

    /**
     * Vytvoří unmarshaller pro načtení dat do xml.
     *
     * @return unmarshaller
     */
    private Unmarshaller createUnmarshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlImport.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller;
    }

    /**
     * Vytvoří schéma.
     *
     * @return schéma
     */
    private Schema createSchema() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Resource resource = applicationContext.getResource("classpath:xsd/xml-import.xsd");
        Schema schema = schemaFactory.newSchema(resource.getFile());
        return schema;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
