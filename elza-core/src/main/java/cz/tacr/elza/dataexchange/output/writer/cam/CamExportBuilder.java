package cz.tacr.elza.dataexchange.output.writer.cam;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;

import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
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

import jakarta.xml.bind.JAXBContext;
import javax.xml.stream.XMLStreamException;

public class CamExportBuilder implements ExportBuilder {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    private EntitiesXml entities;

    private ApStream apStream;

    // static data service or staticDataProvider have to be set
    // for asynchronous workers staticDataProvider has to be request
    // during processing
    private StaticDataService staticDataService;
    private StaticDataProvider staticDataProvider;

    final private GroovyService groovyService;

    final private AccessPointDataService apDataService;

    final private SchemaManager schemaManager;

    // entity filter
    final private boolean applyFilter;

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

    // call by REST
    public CamExportBuilder(final StaticDataProvider staticDataProvider,
                            final GroovyService groovyService,
                            final SchemaManager schemaManager,
                            final AccessPointDataService apDataService,
                            boolean applyFilter) {
        this.staticDataProvider = staticDataProvider;
        this.groovyService = groovyService;
        this.schemaManager = schemaManager;
        this.apDataService = apDataService;
        this.applyFilter = applyFilter;
        initBuilder();
    }

    // call by WDSL
    public CamExportBuilder(final StaticDataService staticDataService,
                            final GroovyService groovyService,
                            final SchemaManager schemaManager,
                            final AccessPointDataService apDataService,
                            boolean applyFilter) {
        this.staticDataService = staticDataService;
        this.groovyService = groovyService;
        this.schemaManager = schemaManager;
        this.apDataService = apDataService;
        this.applyFilter = applyFilter;
        initBuilder();
    }

    private final void initBuilder() {
        this.entities = CamUtils.getObjectFactory().createEntitiesXml();
        Validate.isTrue(apStream == null);
        this.apStream = null;
        if (staticDataService != null) {
            // reset data provider
            // has to be reinitialized at the next transaction
            staticDataProvider = null;
        }
    }

    public void addAccessPoint(ApInfo apInfo) {
        EntityXmlBuilder exb = new EntityXmlBuilder(
                getStaticDataProvider(),
                apInfo.getAccessPoint(),
                apInfo.getApState(),
                apInfo.getExternalIds(),
                groovyService,
                apDataService,
                apInfo.getApState().getScope(),
                applyFilter);

        // Copy received items
        final Map<Integer, List<ApItem>> itemsConv = new HashMap<>();
        Map<Integer, Collection<ApItem>> items = apInfo.getItems();
        if (items != null) {
            items.forEach((a, b) -> itemsConv.put(a, new ArrayList<>(b)));
        }

        EntityXml ent = exb.build(apInfo.getParts(), itemsConv, apInfo.getIndexes());
        this.entities.getList().add(ent);
    }

    /**
     * Return static data provider.
     * 
     * Method will use existing or get new from staticDataService.
     * 
     * @return
     */
    private StaticDataProvider getStaticDataProvider() {
        if (staticDataProvider == null) {
            staticDataProvider = staticDataService.getData();
        }
        return staticDataProvider;
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
