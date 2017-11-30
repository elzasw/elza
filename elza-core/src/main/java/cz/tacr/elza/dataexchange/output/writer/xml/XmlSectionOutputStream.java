package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.PacketStateConvertor;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemAPRefImpl;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemPacketRefImpl;
import cz.tacr.elza.dataexchange.common.items.DescriptionItemPartyRefImpl;
import cz.tacr.elza.dataexchange.output.items.APRefConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemDataConvertorFactory;
import cz.tacr.elza.dataexchange.output.items.PacketRefConvertor;
import cz.tacr.elza.dataexchange.output.items.PartyRefConvertor;
import cz.tacr.elza.dataexchange.output.sections.ExportLevelInfo;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.InternalNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.JaxbNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPointRefs;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Packet;
import cz.tacr.elza.utils.XmlUtils;

/**
 * XML output stream for section export.
 */
class XmlSectionOutputStream implements SectionOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Level.class, Packet.class);

    private final InternalNode parentNode;

    private final XmlFragment levelsFragment;

    private final XmlFragment packetsFragment;

    private final SectionContext sectionContext;

    private boolean processed;

    public XmlSectionOutputStream(InternalNode parentNode, Path tempDirectory, SectionContext sectionContext) {
        this.parentNode = parentNode;
        this.sectionContext = sectionContext;
        this.levelsFragment = new XmlFragment(tempDirectory);
        this.packetsFragment = new XmlFragment(tempDirectory);
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
        // write node APs references
        writeNodeAPs(level, levelInfo.getNodeAPs());
        // write desc items references
        writeItems(level, levelInfo.getItems());

        try {
            writeLevel(level);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void addPacket(ArrPacket packet) {
        Validate.isTrue(!processed);

        Packet element = new Packet();
        element.setId(packet.getPacketId().toString());
        element.setN(packet.getStorageNumber());
        element.setS(PacketStateConvertor.convert(packet.getState()));
        if (packet.getPacketType() != null) {
            element.setT(packet.getPacketType().getCode());
        }
        try {
            writePacket(element);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        Validate.isTrue(!processed);

        try {
            levelsFragment.close();
            packetsFragment.close();
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
            packetsFragment.delete();
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }

    private void writeNodeAPs(Level level, List<ArrNodeRegister> nodeAPs) {
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

    private void writeItems(Level level, List<ArrItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        ItemConvertor convertor = new ItemConvertor(sectionContext.getRuleSystem(),
                new ContextAwareItemDataConvertorFactory());
        List<DescriptionItem> elements = level.getDeOrDiOrDd();
        for (ArrItem item : items) {
            DescriptionItem element = convertor.convert(item);
            elements.add(element);
        }
    }

    private InternalNode getSectionNode() {
        InternalNode sectionNode = new InternalNode(XmlElementName.SECTIONS);
        RulRuleSet ruleSet = sectionContext.getRuleSystem().getRuleSet();
        sectionNode.addAttribute(XmlElementName.RULE_SET_CODE, ruleSet.getCode());

        FundInfo fundInfo = new FundInfo();
        fundInfo.setIc(sectionContext.getInstitutionCode());
        fundInfo.setN(sectionContext.getFundName());
        fundInfo.setC(sectionContext.getFundInternalCode());
        fundInfo.setTr(sectionContext.getTimeRange());
        JAXBElement<?> fiElement = XmlUtils.wrapElement(XmlElementName.FUND_INFO, fundInfo);
        sectionNode.addNode(new JaxbNode(fiElement));

        if (packetsFragment.isExist()) {
            FileNode packetsNode = new FileNode(packetsFragment.getPath());
            sectionNode.addNode(packetsNode);
        }
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
            sw.writeStartElement(XmlElementName.LEVELS);
        }
        XMLStreamWriter sw = levelsFragment.getStreamWriter();
        serializeJaxbType(sw, XmlElementName.LEVEL, level);
    }

    private void writePacket(Packet packet) throws Exception {
        if (!packetsFragment.isOpen()) {
            XMLStreamWriter sw = packetsFragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlElementName.PACKETS);
        }
        XMLStreamWriter sw = packetsFragment.getStreamWriter();
        serializeJaxbType(sw, XmlElementName.PACKET, packet);
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
        public PacketRefConvertor createPacketRefConvertor() {
            return new PacketRefConvertor() {
                @Override
                public DescriptionItemPacketRefImpl convert(ArrData data) {
                    DescriptionItemPacketRefImpl item = super.convert(data);
                    if (item != null) {
                        ArrDataPacketRef packetRef = (ArrDataPacketRef) data;
                        sectionContext.addPacketId(packetRef.getPacketId());
                    }
                    return item;
                }
            };
        }
    }
}
