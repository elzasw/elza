package cz.tacr.elza.dataexchange.output.writer.xml;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.items.*;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * XML output stream for access points export.
 */
public class XmlApOutputStream extends BaseFragmentStream implements ApOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(AccessPoint.class);

    private final ExportContext context;


    private final RootNode rootNode;

    private ItemApConvertor convertor;

    public XmlApOutputStream(RootNode rootNode, Path tempDirectory, ExportContext context) {
        super(tempDirectory);
        this.rootNode = rootNode;
        this.context = context;
        this.convertor = new ItemApConvertor(context.getStaticData(), new ContextAwareItemDataConvertorFactory());
    }

    @Override
    public void addAccessPoint(ApInfo apInfo) {
        Validate.isTrue(!isProcessed());

        AccessPoint element = new AccessPoint();
        element.setApe(createEntry(apInfo.getApState(), apInfo.getExternalIds()));

        // prepare parts

        if (CollectionUtils.isNotEmpty(apInfo.getParts())) {
            Fragments frgs = new Fragments();
            List<ApPart> parentParts = new ArrayList<>();
            for (ApPart part : apInfo.getParts()) {
                Fragment frgElement = new Fragment();
                frgElement.setFid(String.valueOf(part.getPartId()));
                frgElement.setT(part.getPartType().getCode());
                if(apInfo.getItems().get(part.getPartId()) != null) {
                    List<ApItem> itemList = new ArrayList<>(apInfo.getItems().get(part.getPartId()));
                    for (ApItem item : itemList) {
                        DescriptionItem di = convertor.convert(item);
                        frgElement.getDdOrDoOrDp().add(di);
                    }
                }
                if(part.getParentPart() != null) {
                    frgElement.setPid(String.valueOf(part.getParentPart().getPartId()));
                    ApPart tempPart = part;
                    while(tempPart.getParentPart() != null) {
                        parentParts.add(tempPart.getParentPart());
                        tempPart = tempPart.getParentPart();
                    }
                }

                frgs.getFrg().add(frgElement);
            }
            for (ApPart part : parentParts) {
                Fragment frgElement = new Fragment();
                frgElement.setFid(String.valueOf(part.getPartId()));
                frgElement.setT(part.getPartType().getCode());
                if(part.getParentPart() != null) {
                    frgElement.setPid(String.valueOf(part.getParentPart().getPartId()));
                    parentParts.add(part.getParentPart());
                }
                if(apInfo.getItems().get(part.getPartId()) != null) {
                    List<ApItem> itemList = new ArrayList<>(apInfo.getItems().get(part.getPartId()));
                    for (ApItem item : itemList) {
                        DescriptionItem di = convertor.convert(item);
                        frgElement.getDdOrDoOrDp().add(di);
                    }
                }

                frgs.getFrg().add(frgElement);
            }
            element.setFrgs(frgs);
        }

        try {
            writeAP(element);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        finishFragment();

        if (fragment.isExist()) {
            FileNode node = new FileNode(fragment.getPath());
            rootNode.setNode(ChildNodeType.ACCESS_POINTS, node);
        }
    }

    @Override
    public void close() {
        closeFragment();
    }

    private void writeAP(AccessPoint ap) throws Exception {
        if (!fragment.isOpen()) {
            XMLStreamWriter sw = fragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlNameConsts.ACCESS_POINTS);
        }
        XMLStreamWriter sw = fragment.getStreamWriter();
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(XmlNameConsts.PARTY, ap);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, sw);
    }

    public static AccessPointEntry createEntry(ApState apState, Collection<ApBindingState> eids) {
        ApAccessPoint ap = apState.getAccessPoint();

        AccessPointEntry entry = new AccessPointEntry();
        entry.setId(ap.getAccessPointId().toString());
        entry.setT(apState.getApType().getCode());
        entry.setUuid(ap.getUuid());

        // prepare external id
        if (CollectionUtils.isNotEmpty(eids)) {
            List<ExternalId> elementList = entry.getEid();
            for (ApBindingState eid : eids) {
                ExternalId element = new ExternalId();
                element.setV(eid.getBinding().getValue());
                element.setT(eid.getBinding().getApExternalSystem().getCode());
                elementList.add(element);
            }
        }

        return entry;
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
                public DescriptionItemAPRef convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItemAPRef item = super.convert(data, objectFactory);
                    /*if (item != null) {
                        ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                        context.addApId(apRef.getRecordId());
                    }*/
                    return item;
                }
            };
        }
    //TODO : gotzy - zjistit, jestli je to nutne
        /*@Override
        public FileRefConvertor createFileRefConvertor() {
            return new FileRefConvertor() {
                @Override
                public DescriptionItemFileRef convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItemFileRef item = super.convert(data, objectFactory);
                    if (item != null) {
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
        }*/
    }
}
