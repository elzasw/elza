package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.items.APRefConvertor;
import cz.tacr.elza.dataexchange.output.items.FileRefConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemDataConvertorFactory;
import cz.tacr.elza.dataexchange.output.items.StructObjRefConvertor;
import cz.tacr.elza.dataexchange.output.sections.SectionContext;
import cz.tacr.elza.dataexchange.output.writer.FileInfo;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.SectionOutputStream;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.InternalNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.JaxbNode;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemFileRef;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.DigitalArchivalObject;
import cz.tacr.elza.schema.v2.DigitalArchivalObjects;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.ObjectFactory;
import cz.tacr.elza.schema.v2.StructuredObject;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * XML output stream for section export.
 */
class XmlSectionOutputStream implements SectionOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Level.class, StructuredObject.class);

    private final Map<Integer, XmlFragment> structTypeIdFragmentMap = new HashMap<>();

    private final Map<Integer, XmlFragment> fileIdMap = new HashMap<>();

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
    public void addLevel(LevelInfo levelInfo) {
        Validate.isTrue(!processed);

        Level level = EdxOutputHelper.getObjectFactory().createLevel();
        level.setUuid(levelInfo.getNodeUuid());
        level.setId(Integer.toString(levelInfo.getNodeId()));
        if (levelInfo.getParentNodeId() != null) {
            level.setPid(levelInfo.getParentNodeId().toString());
        }
        // convert description items references
        convertItems(levelInfo.getItems(), level.getDdOrDoOrDp());

        // convert daos
        convertDaos(levelInfo.getDaos(), level);

        try {
            writeLevel(level);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private void convertDaos(Collection<ArrDao> daos, Level level) {
        if(daos==null||daos.size()==0) {
            return;
        }
        Validate.isTrue(level.getDaos() == null, "Level already have some DAOs");

        DigitalArchivalObjects xmlDaos = EdxOutputHelper.getObjectFactory().createDigitalArchivalObjects();
        for (ArrDao dao : daos) {
            DigitalArchivalObject xmlDao = EdxOutputHelper.getObjectFactory().createDigitalArchivalObject();
            xmlDao.setDoid(dao.getCode());
            xmlDaos.getDao().add(xmlDao);
        }
        // append only non empty
        if (xmlDaos.getDao().size() > 0) {
            level.setDaos(xmlDaos);
        }
    }

    @Override
    public void addFile(FileInfo fileInfo) {
        Validate.isTrue(!processed);

        XmlFragment frag = fileIdMap.get(fileInfo.getId());
        if (frag == null) {
            frag = new XmlFragment(tempDirectory);

            try {
                XMLStreamWriter sw = frag.openStreamWriter();
                sw.writeStartDocument();
                sw.writeStartElement(XmlNameConsts.FILE);
                sw.writeAttribute(XmlNameConsts.FILE_ID, fileInfo.getId().toString());
                sw.writeAttribute(XmlNameConsts.FILE_NAME, fileInfo.getName());
                sw.writeAttribute(XmlNameConsts.FILE_FILENAME, fileInfo.getFileName());
                sw.writeAttribute(XmlNameConsts.FILE_MIMETYPE, fileInfo.getMimetype());

                // write binary data
                sw.writeStartElement(XmlNameConsts.FILE_BIN_DATA);
                try (InputStream is = fileInfo.getInputStream()) {
                    writeBinaryFileData(sw, is);
                }
                sw.writeEndElement();

                sw.writeEndElement();
                //sw.writeEndDocument();
            } catch (Exception e) {
                try {
                    frag.close();
                } catch (Exception e2) {
                }
                throw new SystemException(e);
            }

            // close fragment
            try {
                frag.close();
            } catch (Exception e) {
                throw new SystemException(e);
            }
            fileIdMap.put(fileInfo.getId(), frag);
        }
    }

    private void writeBinaryFileData(XMLStreamWriter sw, InputStream is) throws IOException, XMLStreamException {
        try (Base64InputStream encoder = new Base64InputStream(is, true, Integer.MAX_VALUE, null)) {
            byte buff[] = new byte[65536];
            while (encoder.available() > 0) {
                int numRead = encoder.read(buff);
                if (numRead < 0) {
                    break;
                }
                String str = new String(buff, 0, numRead, Charset.forName("utf-8"));
                sw.writeCharacters(str);
            }
        }
    }

    @Override
    public void addStructObject(StructObjectInfo structObjInfo) {
        Validate.isTrue(!processed);

        StructuredObject structObj = new StructuredObject();
        structObj.setId(Integer.toString(structObjInfo.getId()));
        structObj.setAsgn(structObjInfo.getAssignable());
        structObj.setUuid(structObjInfo.getUuid());
        structObj.setV(structObjInfo.getValue());
        structObj.setCmpl(structObjInfo.getComplement());

        // convert description items references
        //TODO: gotzy convertItems(structObjInfo.getItems(), structObj.getDdOrDoOrDn());
        convertItems(structObjInfo.getItems(), structObj.getDdOrDoOrDp());

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
        } catch (IOException e) {
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

    private void convertItems(Collection<? extends ArrItem> items, List<DescriptionItem> outList) {
        if (items == null || items.isEmpty()) {
            return;
        }
        ItemConvertor convertor = new ItemConvertor(sectionContext.getStaticData(),
                new ContextAwareItemDataConvertorFactory());
        for (ArrItem item : items) {
            DescriptionItem element = convertor.convert(item);
            outList.add(element);
        }
    }

    private InternalNode getSectionNode() {
        InternalNode sectionNode = new InternalNode(XmlNameConsts.SECTION);
        RulRuleSet ruleSet = sectionContext.getRuleSet();
        sectionNode.addAttribute(XmlNameConsts.RULE_SET_CODE, ruleSet.getCode());

        FundInfo fundInfo = new FundInfo();
        fundInfo.setIc(sectionContext.getInstitutionCode());
        fundInfo.setN(sectionContext.getFundName());
        fundInfo.setC(sectionContext.getFundInternalCode());
        fundInfo.setMrk(sectionContext.getFundMark());
        fundInfo.setNum(sectionContext.getFundNumber());
        fundInfo.setTr(sectionContext.getFundTimeRange());
        JAXBElement<?> fiElement = XmlUtils.wrapElement(XmlNameConsts.FUND_INFO, fundInfo);
        sectionNode.addNode(new JaxbNode(fiElement));

        // create xml node for structured types
        if (!structTypeIdFragmentMap.isEmpty()) {
            InternalNode stsNode = new InternalNode(XmlNameConsts.STRUCT_TYPES);
            structTypeIdFragmentMap.values().forEach(stFragment -> {
                FileNode stNode = new FileNode(stFragment.getPath());
                stsNode.addNode(stNode);
            });
            sectionNode.addNode(stsNode);
        }

        // create xml node for files
        if (!fileIdMap.isEmpty()) {
            InternalNode filesNode = new InternalNode(XmlNameConsts.FILES);
            fileIdMap.values().forEach(fileFrag -> {
                FileNode fileNode = new FileNode(fileFrag.getPath());
                filesNode.addNode(fileNode);
            });
            sectionNode.addNode(filesNode);
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

    private void writeStructObject(StructuredObject structObj, RulStructuredType structType) throws Exception {
        XmlFragment frag = structTypeIdFragmentMap.get(structType.getStructuredTypeId());
        if (frag == null) {
            frag = new XmlFragment(tempDirectory);
            try {
                XMLStreamWriter sw = frag.openStreamWriter();
                sw.writeStartDocument();
                sw.writeStartElement(XmlNameConsts.STRUCT_TYPE);
                sw.writeAttribute(XmlNameConsts.STRUCT_TYPE_CODE, structType.getCode());
                sw.writeStartElement(XmlNameConsts.STRUCT_OBJECTS);
            } catch (Exception e) {
                frag.close();
                throw e;
            }
            structTypeIdFragmentMap.put(structType.getStructuredTypeId(), frag);
        }

        XMLStreamWriter sw = frag.getStreamWriter();
        serializeJaxbType(sw, XmlNameConsts.STRUCT_OBJECT, structObj);
    }

    private void serializeJaxbType(XMLStreamWriter streamWriter, String localName, Object value) throws JAXBException {
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(localName, value);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, streamWriter);
    }

    /**
     * Rozsireni standardnich convertoru o kontext exportu
     *
     * Umoznuje prevadet slozitejsi typy
     */
    private class ContextAwareItemDataConvertorFactory extends ItemDataConvertorFactory {

        @Override
        public APRefConvertor createAPRefConvertor() {
            return new APRefConvertor() {
                @Override
                public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItem item = super.convert(data, objectFactory);
                    if (item != null && item instanceof DescriptionItemAPRef) {
                        ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                        if (apRef.getRecordId() != null) {
                            sectionContext.getContext().addApId(apRef.getRecordId());
                        }
                    }
                    return item;
                }
            };
        }

        @Override
        public FileRefConvertor createFileRefConvertor() {
            return new FileRefConvertor() {
                @Override
                public DescriptionItem convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItem item = super.convert(data, objectFactory);
                    // item might be null or DescriptionItemUndefined if undefined
                    if (item != null && item instanceof DescriptionItemFileRef) {
                        ArrDataFileRef fileRef = (ArrDataFileRef) data;
                        sectionContext.addDmsFile(fileRef.getFileId());
                    }
                    return item;
                }
            };
        }

        @Override
        public StructObjRefConvertor createStructObjectRefConvertor() {
            return new StructObjRefConvertor() {
                @Override
                public DescriptionItemStructObjectRef convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItemStructObjectRef item = super.convert(data, objectFactory);
                    if (item != null) {
                        ArrDataStructureRef structObjRef = (ArrDataStructureRef) data;
                        sectionContext.addStructObjectId(structObjRef.getStructuredObjectId());
                    }
                    return item;
                }
            };
        }
    }
}
