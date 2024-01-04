package cz.tacr.elza.print;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.Validate;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.aps.AccessPointsReader;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.context.ExportInitHelper;
import cz.tacr.elza.dataexchange.output.writer.cam.CamExportBuilder;
import cz.tacr.elza.dataexchange.output.writer.cam.CamUtils;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.ap.ExternalId;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.print.part.Part;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;
import cz.tacr.elza.ws.core.v1.ExportRequestException;

/**
 * One record from registry
 * <p>
 * Each record has its type, record name and characteristics
 */
public class Record {

    private final ApAccessPoint ap;

    final private ApState apState;

    private final RecordType type;

    private final StaticDataProvider staticData;

    private final ApBindingRepository bindingRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApItemRepository itemRepository;

    private final ApIndexRepository indexRepository;
    
    private final ElzaLocale elzaLocale;

    private List<ExternalId> eids;

    private List<Part> parts;

    private Part preferredPart;

    private OutputItemConvertor outputItemConvertor;

    final private OutputContext outputContext;

    public Record(final OutputContext outputContext,
                  final ApState apState,
                  final RecordType type,
                  final ApBindingRepository bindingRepository,
                  final ApItemRepository itemRepository,
                  final ApBindingStateRepository bindingStateRepository,
                  final ApIndexRepository indexRepository,
                  final OutputItemConvertor outputItemConvertor,
                  final ElzaLocale elzaLocale) {
        this.outputContext = outputContext;
        this.apState = apState;
        this.ap = apState.getAccessPoint();
        this.type = type;
        this.staticData = outputContext.getStaticData();
        this.bindingRepository = bindingRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.indexRepository = indexRepository;
        this.outputItemConvertor = outputItemConvertor;
        this.elzaLocale = elzaLocale;
    }

    /**
     * Copy constructor
     */
    protected Record(Record src) {
        this.outputContext = src.outputContext;
        this.apState = src.apState;
        this.ap = src.ap;
        this.type = src.type;
        this.staticData = src.staticData;
        this.bindingRepository = src.bindingRepository;
        this.itemRepository = src.itemRepository;
        this.eids = src.eids;
        this.preferredPart = src.preferredPart;
        this.parts = src.parts;
        this.bindingStateRepository = src.bindingStateRepository;
        this.indexRepository = src.indexRepository;
        this.outputItemConvertor = src.outputItemConvertor;
        this.elzaLocale = src.elzaLocale;
    }

    private void loadData(List<CachedPart> cachedParts) {

        // sorting parts
        Map<CachedPart, String> sortValues = cachedParts.stream().collect(Collectors.toMap(p -> p, p -> ApFactory.getSortName(p)));
        cachedParts.sort((p1, p2) -> {
            String s1 = sortValues.get(p1);
            String s2 = sortValues.get(p2);
            if (s1 == null || s2 == null) {
                return 0;
            }
            return elzaLocale.getCollator().compare(s1, s2);
        });

        List<Part> subParts = new ArrayList<>();
        Map<Integer, Part> partIdMap = new HashMap<>();

        // prepare parts
        parts = new ArrayList<>(cachedParts.size());
        for (CachedPart cachedPart : cachedParts) {
            Part part = createPart(cachedPart);

            partIdMap.put(cachedPart.getPartId(), part);

            if (part.getParentPartId() != null) {
                subParts.add(part);
            } else {
                // set preferred part
                if (ap.getPreferredPartId().equals(cachedPart.getPartId())) {
                    preferredPart = part;
                }
                parts.add(part);
            }
        }
        parts = Collections.unmodifiableList(parts);

        Map<Part, List<Part>> subPartMap = new HashMap<>();
        // process sub parts
        for (Part subPart : subParts) {
            Part parent = partIdMap.get(subPart.getParentPartId());
            Validate.notNull(parent, "Parent part not found, partId = %i", subPart.getParentPartId());
            List<Part> partList = subPartMap.computeIfAbsent(parent, p -> new ArrayList<>());
            partList.add(subPart);
        }
        // store subparts
        subPartMap.forEach((part, list) -> part.setParts(list));
    }

    private Part createPart(CachedPart cachedPart) { 
        String index = ApFactory.findIndexValue(cachedPart.getIndices(), DISPLAY_NAME);
        Part part = new Part(cachedPart, ap.getAccessPointId(), staticData, index);            
        // process items
        List<ApItem> apItems = cachedPart.getItems();
        if (!CollectionUtils.isEmpty(apItems)) {
            for (ApItem apItem : apItems) {
                Item item = outputItemConvertor.convert(apItem);
                part.addItem(item);
            }
        }
        return part;
    }

    private void loadParts() {
        if (parts != null) {
            return;
        }

        CachedAccessPoint cachedAccessPoint = this.outputContext.findCachedAccessPoint(ap.getAccessPointId());
        if (cachedAccessPoint == null) {
            throw new IllegalStateException("ApAccessPoint not found in CachedAccessPoint, apAccessPointId=" + ap.getAccessPointId());
        }
        loadData(cachedAccessPoint.getParts());
    }

    public Integer getId() {
        return ap.getAccessPointId();
    }

    public String getUuid() {
        return ap.getUuid();
    }

    public RecordType getType() {
        return type;
    }

    public List<ExternalId> getEids() {
        if (eids == null) {
            List<ApBindingState> apEids = bindingStateRepository.findByAccessPoint(ap);
            eids = new ArrayList<>(apEids.size());
            for (ApBindingState apEid : apEids) {
                ExternalId eid = ExternalId.newInstance(apEid.getBinding(), staticData);
                eids.add(eid);
            }
            // make external ids read-only
            eids = Collections.unmodifiableList(eids);
        }
        return eids;
    }

    /**
     * Return string with formatted list of external ids
     * <p>
     * Format of the result is <type1>: <value1>, <type2>: <value2>...
     */
    public String getFormattedEids() {
        List<ExternalId> eids = getEids();
        if (eids == null) {
            return "";
        } else {
            return eids.stream()
                    .filter(eid -> !eid.getType().getType().equals(ApExternalSystemType.CAM_UUID))
                    .map(eid -> {
                return eid.getType().getName() + ": " + eid.getValue();
            }).collect(Collectors.joining(", "));
        }
    }

    public List<Part> getParts() {
        loadParts();

        return parts;
    }

    /**
     * Return single part
     * 
     * @param partTypeCode
     * @return
     */
    public Part getPart(final String partTypeCode) {
        final List<Part> parts = getParts(partTypeCode);
        if (parts.size() == 0) {
            return null;
        }
        if (parts.size() > 1) {
            throw new BusinessException("Multiple parts of required type exists.", BaseCode.INVALID_STATE)
                    .set("partTypeCode", partTypeCode)
                    .set("name", preferredPart.getValue())
                    .set("count", parts.size());
        }
        return parts.get(0);
    }

    public List<Part> getParts(final String partTypeCode) {
        return getParts(Collections.singletonList(partTypeCode));
    }

    public List<Part> getParts(final Collection<String> partTypeCodes) {
        Validate.notNull(partTypeCodes);
        loadParts();

        return parts.stream().filter(part -> {
            String partTypeCode = part.getPartType().getCode();
            return partTypeCodes.contains(partTypeCode);
        }).collect(Collectors.toList());
    }

    public List<Item> getItems() {
        loadParts();

        List<Item> itemList = new ArrayList<>();
        for (Part part : parts) {
            itemList.addAll(part.getItems());
        }
        return itemList;
    }

    public List<Item> getItems(Collection<String> itemTypeCodes) {
        Validate.notNull(itemTypeCodes);

        if (parts == null || itemTypeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Item> itemList = new ArrayList<>();
        for (Part part : parts) {
            itemList.addAll(part.getItems().stream().filter(item -> {
                String itemTypeCode = item.getType().getCode();
                return itemTypeCodes.contains(itemTypeCode);
            }).collect(Collectors.toList()));
        }
        return itemList;
    }

    public Part getPreferredPart() {
        loadParts();

        return preferredPart;
    }

    public void setPreferredPart(Part preferredPart) {
        this.preferredPart = preferredPart;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    /**
     * Export entity in given format
     * 
     * @param nameSpace
     * @return
     * @throws XMLStreamException
     * @throws ParserConfigurationException
     */
    public String exportData(String format) throws Exception {
        if (CamUtils.CAM_SCHEMA.equals(format)) {
            return exportDataCam();
        } else {
            throw new ExportRequestException("Unrecognized schema: " + format);
        }
    }

    private String exportDataCam() throws Exception {
        /*
        // fomat CAM
        CamExportBuilder exportBuilder = new CamExportBuilder(staticData, outputContext.getGroovyService(),
                outputContext.getSchemaManager(),
                outputContext.getApDataService());

        ExportContext ec = new ExportContext(exportBuilder, outputContext.getStaticData(), 1);
        ec.addApId(this.ap.getAccessPointId());

        ExportInitHelper eih = outputContext.getExportInitHelper();
        AccessPointsReader apr = new AccessPointsReader(ec, eih);

        apr.read();

        exportBuilder.buildEntity(writer);
        */
        Document xmlDoc = exportXmlDataCam();
        // rename namespace to cam        
        LinkedList<org.w3c.dom.Node> xmlNodes = new LinkedList<>();
        xmlNodes.add(xmlDoc);
        while (xmlNodes.size() > 0) {
            org.w3c.dom.Node n = xmlNodes.pop();
            NodeList childNodes = null;
            if (n.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
                Document doc = (Document) n;
                childNodes = doc.getChildNodes();
            } else
            if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element el = (Element) n;
                String nsUri = el.getNamespaceURI();
                if (CamUtils.CAM_SCHEMA.equals(nsUri) && el.getPrefix() == null) {
                    el.setPrefix("cam");
                }
                // remove xmlns declaration
                if (el.hasAttribute("xmlns")) {
                    el.removeAttribute("xmlns");
                }
                childNodes = el.getChildNodes();
            }
            if (childNodes != null) {
                for (int i = 0; i < childNodes.getLength(); i++) {
                    xmlNodes.add(childNodes.item(i));
                }
            }
        }

        StringWriter writer = new StringWriter();
        StreamResult sr = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        //transform document to string 
        transformer.transform(new DOMSource(xmlDoc.getFirstChild()), sr);

        return writer.toString();
    }

    public org.w3c.dom.Node exportXmlData(String format) throws XMLStreamException, ParserConfigurationException {
        if (CamUtils.CAM_SCHEMA.equals(format)) {
            return exportXmlDataCam();
        } else {
            throw new ExportRequestException("Unrecognized schema: " + format);
        }
    }

    private Document exportXmlDataCam() throws XMLStreamException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder db = factory.newDocumentBuilder();
        Document xmlDoc = db.newDocument();

        // format CAM
        CamExportBuilder exportBuilder = new CamExportBuilder(staticData, outputContext.getGroovyService(),
                outputContext.getSchemaManager(),
                outputContext.getApDataService(),
                true);

        ExportContext expCtx = new ExportContext(exportBuilder, outputContext.getStaticData(), 1);
        // set flags include AP && UUID
        expCtx.setIncludeAccessPoints(true);
        expCtx.setIncludeUUID(true);

        expCtx.addApId(this.ap.getAccessPointId());

        ExportInitHelper eih = outputContext.getExportInitHelper();
        AccessPointsReader apr = new AccessPointsReader(expCtx, eih);

        apr.read();

        exportBuilder.buildEntity(xmlDoc);

        return xmlDoc;
    }
}
