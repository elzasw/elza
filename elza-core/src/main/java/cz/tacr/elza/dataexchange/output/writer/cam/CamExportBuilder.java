package cz.tacr.elza.dataexchange.output.writer.cam;

import java.io.OutputStream;
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

import cz.tacr.elza.service.AccessPointDataService;
import org.apache.commons.lang3.Validate;

import cz.tacr.cam.schema.cam.EntitiesXml;
import cz.tacr.cam.schema.cam.EntityXml;
import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.schema.SchemaManager;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.ExportBuilder;
// import cz.tacr.elza.dataexchange.output.writer.PartiesOutputStream;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.service.GroovyService;
import cz.tacr.elza.service.cam.CamXmlFactory;
import cz.tacr.elza.service.cam.EntityXmlBuilder;

public class CamExportBuilder implements ExportBuilder {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(EntitiesXml.class);

    private EntitiesXml entities;

    private ApStream apStream;

    final private StaticDataService staticDataService;

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

    public CamExportBuilder(final StaticDataService staticDataService,
                            final GroovyService groovyService,
                            final SchemaManager schemaManager,
                            final AccessPointDataService apDataService) {
        this.staticDataService = staticDataService;
        this.groovyService = groovyService;
        this.schemaManager = schemaManager;
        this.apDataService = apDataService;
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
                groovyService,
                apDataService,
                apInfo.getApState().getScope(),
                ApExternalSystemType.CAM);

        final Map<Integer, List<ApItem>> itemsConv = new HashMap<>();
        Map<Integer, Collection<ApItem>> items = apInfo.getItems();
        if (items != null) {
            items.forEach((a, b) -> itemsConv.put(a, new ArrayList<>(b)));
        }

        EntityXml ent = exb.build(apInfo.getParts(), itemsConv);
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
            marshaller.setSchema(schemaManager.getSchema(SchemaManager.CAM_SCHEMA_URL));
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
