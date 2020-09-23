package cz.tacr.elza.dataexchange.output.writer.cam;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.service.GroovyService;
import org.apache.commons.lang3.Validate;
import org.xml.sax.SAXException;

import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
// import cz.tacr.elza.dataexchange.output.writer.PartiesOutputStream;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.cam.CamXmlFactory;
import cz.tacr.elza.service.cam.EntityXmlBuilder;

public class CamExportBuilder implements ExportBuilder {

    static Schema camSchema;
    {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream is = CamExportBuilder.class.getClassLoader().getResourceAsStream("cam/cam-2019.xsd")) {
            camSchema = sf.newSchema(new StreamSource(is));
        } catch (IOException | SAXException e) {
            throw new RuntimeException("Failed to load internal XSD", e);
        }
    }

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    private EntitiesXml entities;

    private ApStream apStream;

    private StaticDataService staticDataService;

    private GroovyService groovyService;

    protected CamExportBuilder getExportBuilder() {
        return this;
    }

    class ApStream implements ApOutputStream {

        @Override
        public void addAccessPoint(ApInfo apInfo) {
            CamExportBuilder expBuilder = getExportBuilder();
            expBuilder.addAccessPoint(apInfo);
        }

        @Override
        public void processed() {
            // nop
        }

        @Override
        public void close() {
            CamExportBuilder expBuilder = getExportBuilder();
            Validate.notNull(expBuilder.apStream);
            expBuilder.apStream = null;
        }

    };

    public CamExportBuilder(StaticDataService staticDataService, GroovyService groovyService) {
        this.staticDataService = staticDataService;
        this.groovyService = groovyService;
        initBuilder();
    }

    private final void initBuilder() {
        this.entities = CamXmlFactory.getObjectFactory().createEntitiesXml();
        Validate.isTrue(apStream == null);
        this.apStream = null;
    }

    public void addAccessPoint(ApInfo apInfo) {
        EntityXmlBuilder exb = new EntityXmlBuilder(
                staticDataService.getData(),
                apInfo.getAccessPoint(),
                apInfo.getApState(),
                groovyService);

        Map<Integer, Collection<ApItem>> items = apInfo.getItems();
        final Map<Integer, List<ApItem>> itemsConv = new HashMap<>();
        items.forEach((a, b) -> itemsConv.put(a, new ArrayList<>(b)));

        EntityXml ent = exb.build(apInfo.getParts(), itemsConv, ApExternalSystemType.CAM.toString());
        this.entities.getList().add(ent);
    }

    /* private void addAPName(Entity ent, ApName apName) {
    }*/

    @Override
    public SectionOutputStream openSectionOutputStream(SectionContext sectionContext) {
        throw new SystemException("CAM format does not support sections");
    }

    @Override
    public ApOutputStream openAccessPointsOutputStream(ExportContext exportContext) {
        Validate.isTrue(apStream == null);
        if (apStream == null) {
            apStream = new ApStream();
        }
        return apStream;
    }

    @Override
    public void build(OutputStream os) throws XMLStreamException {
        JAXBElement<EntitiesXml> jaxbEnts = CamXmlFactory.getObjectFactory().createEnts(this.entities);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(camSchema);
            marshaller.marshal(jaxbEnts, os);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to save with JAXB", e);
        }
    }

    @Override
    public void clear() {
        initBuilder();
    }

}
