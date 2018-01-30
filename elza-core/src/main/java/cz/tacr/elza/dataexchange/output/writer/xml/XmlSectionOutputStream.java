package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemAPRefImpl;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemPartyRefImpl;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemStructObjectRefImpl;
import cz.tacr.elza.dataexchange.output.items.APRefConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemDataConvertorFactory;
import cz.tacr.elza.dataexchange.output.items.PartyRefConvertor;
import cz.tacr.elza.dataexchange.output.items.StructObjRefConvertor;
import cz.tacr.elza.dataexchange.output.sections.ExportLevelInfo;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfo;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.InternalNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.JaxbNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPointRefs;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.StructuredObject;

/**
 * XML output stream for section export.
 */
class XmlSectionOutputStream implements SectionOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Level.class, StructuredObject.class);

    private final Map<Integer, XmlFragment> structTypeIdFragmentMap = new HashMap<>();

    private final InternalNode parentNode;

    private final XmlFragment levelsFragment;

    private final SectionContext sectionContext;

    private final Path tempDirectory;

    private boolean processed;

    public XmlSectionOutputStream(InternalNode parentNode, Path tempDirectory, SectionContext sectionContext) {
        this.parentNode = parentNode;
        this.sectionContext = sectionContext;
        this.levelsFragment = new XmlFragment(tempDirectory);
        this.tempDirectory = tempDirectory;
    }

    @Override
    public void addLevel(ExportLevelInfo levelInfo) {
        Validate.isTrue(!processed);

        Level level = new Level();
        level.setUuid(levelInfo.getNodeUuid());
        level.setId(Integer.toString(levelInfo.getNodeId()));
        if (levelInfo.getParentNodeId() != null) {
            level.setPid(levelInfo.getParentNodeId().toString());
        }
        // convert node APs references
        convertNodeAPs(levelInfo.getNodeAPs(), level);
        // convert desc items references
        convertItems(levelInfo.getItems(), level.getDeOrDiOrDd());

        try {
            writeLevel(level);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void addStructObject(StructObjectInfo structObjInfo) {
        Validate.isTrue(!processed);

        StructuredObject structObj = new StructuredObject();
        structObj.setId(Integer.toString(structObjInfo.getId()));

        // convert desc items references
        convertItems(structObjInfo.getItems(), structObj.getDeOrDiOrDd());

        try {
            writeStructObject(structObj, structObjInfo.getStructType());
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        Validate.isTrue(!processed);

        try {
            levelsFragment.close();
            for (XmlFragment stFragment : structTypeIdFragmentMap.values()) {
                stFragment.close();
            }
        } catch (XMLStreamException | IOException e) {
            throw new SystemException(e);
        }

        InternalNode sn = getSectionNode();
        parentNode.addNode(sn);
        processed = true;
    }

    @Override
    public void close() {
        if (processed) {
            return;
        }
        try {
            levelsFragment.delete();
            for (XmlFragment stFragment : structTypeIdFragmentMap.values()) {
                stFragment.delete();
            }
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void convertNodeAPs(Collection<ArrNodeRegister> nodeAPs, Level level) {
        if (nodeAPs == null || nodeAPs.isEmpty()) {
            return;
        }
        AccessPointRefs apRefs = new AccessPointRefs();
        List<String> apIds = apRefs.getApid();
        level.setAprs(apRefs);
        for (ArrNodeRegister nodeAP : nodeAPs) {
            sectionContext.getContext().addAPId(nodeAP.getRecordId());
            apIds.add(nodeAP.getRecordId().toString());
        }
    }

    private void convertItems(Collection<? extends ArrItem> items, List<DescriptionItem> outList) {
        if (items == null || items.isEmpty()) {
            return;
        }
        ItemConvertor convertor = new ItemConvertor(sectionContext.getRuleSystem(), new ContextAwareItemDataConvertorFactory());
        for (ArrItem item : items) {
            DescriptionItem element = convertor.convert(item);
            outList.add(element);
        }
    }

    private InternalNode getSectionNode() {
        InternalNode sectionNode = new InternalNode(XmlNameConsts.SECTIONS);
        RulRuleSet ruleSet = sectionContext.getRuleSystem().getRuleSet();
        sectionNode.addAttribute(XmlNameConsts.RULE_SET_CODE, ruleSet.getCode());

        FundInfo fundInfo = new FundInfo();
        fundInfo.setIc(sectionContext.getInstitutionCode());
        fundInfo.setN(sectionContext.getFundName());
        fundInfo.setC(sectionContext.getFundInternalCode());
        fundInfo.setTr(sectionContext.getTimeRange());
        JAXBElement<?> fiElement = XmlUtils.wrapElement(XmlNameConsts.FUND_INFO, fundInfo);
        sectionNode.addNode(new JaxbNode(fiElement));

        // create xml node for structured types
        if (!structTypeIdFragmentMap.isEmpty()) {
            InternalNode stsNode = new InternalNode(XmlNameConsts.STRUCT_TYPES);
            structTypeIdFragmentMap.values().forEach(stFragment -> {
                FileNode stNode = new FileNode(stFragment.getPath());
                stsNode.addNode(stNode);
            });
        }
        // create xml node for levels
        if (levelsFragment.isExist()) {
            FileNode levelsNode = new FileNode(levelsFragment.getPath());
            sectionNode.addNode(levelsNode);
        }
        return sectionNode;
    }

    private void writeLevel(Level level) throws Exception {
        if (!levelsFragment.isOpen()) {
            XMLStreamWriter sw = levelsFragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlNameConsts.LEVELS);
        }
        XMLStreamWriter sw = levelsFragment.getStreamWriter();
        serializeJaxbType(sw, XmlNameConsts.LEVEL, level);
    }

    private void writeStructObject(StructuredObject structObj, RulStructureType structType) throws Exception {
        XmlFragment structTypeFragment = structTypeIdFragmentMap.get(structType.getStructureTypeId());
        if (structTypeFragment == null) {
            structTypeFragment = new XmlFragment(tempDirectory);
            XMLStreamWriter sw = structTypeFragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlNameConsts.STRUCT_TYPE);
            sw.writeAttribute(XmlNameConsts.STRUCT_TYPE_CODE, structType.getName());
            sw.writeStartElement(XmlNameConsts.STRUCT_OBJECTS);
            structTypeIdFragmentMap.put(structType.getStructureTypeId(), structTypeFragment);
        }

        XMLStreamWriter sw = structTypeFragment.getStreamWriter();
        serializeJaxbType(sw, XmlNameConsts.STRUCT_OBJECT, structObj);
    }

    private void serializeJaxbType(XMLStreamWriter streamWriter, String localName, Object value) throws JAXBException {
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(localName, value);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, streamWriter);
    }

    private class ContextAwareItemDataConvertorFactory extends ItemDataConvertorFactory {

        @Override
        public APRefConvertor createAPRefConvertor() {
            return new APRefConvertor() {
                @Override
                public DescriptionItemAPRefImpl convert(ArrData data) {
                    DescriptionItemAPRefImpl item = super.convert(data);
                    if (item != null) {
                        ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                        sectionContext.getContext().addAPId(apRef.getRecordId());
                    }
                    return item;
                }
            };
        }

        @Override
        public PartyRefConvertor createPartyRefConvertor() {
            return new PartyRefConvertor() {
                @Override
                public DescriptionItemPartyRefImpl convert(ArrData data) {
                    DescriptionItemPartyRefImpl item = super.convert(data);
                    if (item != null) {
                        ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                        sectionContext.getContext().addPartyId(partyRef.getPartyId());
                    }
                    return item;
                }
            };
        }

        @Override
        public StructObjRefConvertor createStructObjectRefConvertor() {
            return new StructObjRefConvertor() {
                @Override
                public DescriptionItemStructObjectRefImpl convert(ArrData data) {
                    DescriptionItemStructObjectRefImpl item = super.convert(data);
                    if (item != null) {
                        ArrDataStructureRef structObjRef = (ArrDataStructureRef) data;
                        sectionContext.addStructObjectId(structObjRef.getStructureDataId());
                    }
                    return item;
                }
            };
        }
    }
}
