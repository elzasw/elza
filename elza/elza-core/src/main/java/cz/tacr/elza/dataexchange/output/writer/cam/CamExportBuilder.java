package cz.tacr.elza.dataexchange.output.writer.cam;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;

import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.AccessPointDataService;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.cam.EntityXmlBuilder;

public class CamExportBuilder implements ExportBuilder {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    private EntitiesXml entities;

    private ApStream apStream;

    final private StaticDataProvider staticDataSProvider;

    final private GroovyService groovyService;

    final private AccessPointDataService apDataService;

    final private SchemaManager schemaManager;

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

    public CamExportBuilder(final StaticDataProvider staticDataSProvider,
                            final GroovyService groovyService,
                            final SchemaManager schemaManager,
                            final AccessPointDataService apDataService) {
        this.staticDataSProvider = staticDataSProvider;
        this.groovyService = groovyService;
        this.schemaManager = schemaManager;
        this.apDataService = apDataService;
        initBuilder();
    }

    private final void initBuilder() {
        this.entities = CamUtils.getObjectFactory().createEntitiesXml();
        Validate.isTrue(apStream == null);
        this.apStream = null;
    }

    public void addAccessPoint(ApInfo apInfo) {
        EntityXmlBuilder exb = new EntityXmlBuilder(
                staticDataSProvider,
                apInfo.getAccessPoint(),
                apInfo.getApState(),
                apInfo.getExternalIds(),
                groovyService,
                apDataService,
                apInfo.getApState().getScope());

        // Copy received items
        final Map<Integer, List<ApItem>> itemsConv = new HashMap<>();
        Map<Integer, Collection<ApItem>> items = apInfo.getItems();
        if (items != null) {
            items.forEach((a, b) -> itemsConv.put(a, new ArrayList<>(b)));
        }

        EntityXml ent = exb.build(apInfo.getParts(), itemsConv, apInfo.getIndexes());
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
        JAXBElement<EntitiesXml> jaxbEnts = CamUtils.getObjectFactory().createEnts(this.entities);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setSchema(schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL));
            marshaller.marshal(jaxbEnts, os);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to save with JAXB", e);
        }
    }

    public void buildEntity(Writer writer) throws XMLStreamException {
        Validate.isTrue(this.entities.getList().size() == 1);
        EntityXml entXml = this.entities.getList().get(0);

        JAXBElement<EntityXml> jaxbEnt = CamUtils.getObjectFactory().createEnt(entXml);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setSchema(schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL));
            marshaller.marshal(jaxbEnt, writer);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to save with JAXB", e);
        }
    }

    public void buildEntity(Document xmlDoc) throws XMLStreamException {
        Validate.isTrue(this.entities.getList().size() == 1);
        EntityXml entXml = this.entities.getList().get(0);

        JAXBElement<EntityXml> jaxbEnt = CamUtils.getObjectFactory().createEnt(entXml);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setSchema(schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL));
            marshaller.marshal(jaxbEnt, xmlDoc);
        } catch (JAXBException e) {
            throw new XMLStreamException("Failed to save with JAXB", e);
        }
    }

    @Override
    public void clear() {
        initBuilder();
    }

    @Override
    public boolean canExportDeletedAPs() {
        return true;
    }

}
