package cz.tacr.elza.dataexchange.output.writer.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.context.ExportContext;
import cz.tacr.elza.dataexchange.output.items.ItemApConvertor;
import cz.tacr.elza.dataexchange.output.items.ItemDataConvertorFactory;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointState;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.ExternalId;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

import javax.xml.stream.XMLStreamWriter;

/**
 * XML output stream for access points export.
 */
public class XmlApOutputStream extends BaseFragmentStream implements ApOutputStream {

    private final Logger logger = LoggerFactory.getLogger(XmlApOutputStream.class);

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(AccessPoint.class);

    private final ExportContext context;


    private final RootNode rootNode;

    private ItemApConvertor convertor;

    public XmlApOutputStream(RootNode rootNode, Path tempDirectory, ExportContext context) {
        super(tempDirectory);
        this.rootNode = rootNode;
        this.context = context;
        this.convertor = new ItemApConvertor(context.getStaticData(), new ContextAwareItemDataConvertorFactory());

        logger.debug("New XmlApOutputStream, tempDirectory: {}", tempDirectory);
    }

    @Override
    public void addAccessPoint(ApInfo apInfo) {
        logger.debug("Received accesspoint info, accessPointId: {}, uuid: {}",
                     apInfo.getAccessPoint().getAccessPointId(),
                     apInfo.getAccessPoint().getUuid());

        Validate.isTrue(!isProcessed());

        AccessPoint element = new AccessPoint();
        element.setApe(createEntry(apInfo.getApState(), apInfo.getExternalIds()));

        // prepare parts
        if (CollectionUtils.isNotEmpty(apInfo.getParts())) {
            Fragments frgs = new Fragments();
            List<ApPart> subParts = new ArrayList<>();
            // export main / parent parts
            for (ApPart part : apInfo.getParts()) {
                if (part.getParentPart() != null) {
                    subParts.add(part);
                    continue;
                }

                Fragment frgElement = createFragment(part, apInfo.getItems().get(part.getPartId()));
                frgs.getFrg().add(frgElement);
            }
            // export subparts
            for (ApPart part : subParts) {
                Fragment frgElement = createFragment(part, apInfo.getItems().get(part.getPartId()));
                frgs.getFrg().add(frgElement);
            }
            element.setFrgs(frgs);
        }

        try {
            writeAP(element);
        } catch (Exception e) {
            logger.error("Failed to write AP", e);
            throw new SystemException(e);
        }
    }

    /**
     * Create element for one part
     *
     * @param part
     * @param items
     * @return
     */
    private Fragment createFragment(ApPart part, Collection<ApItem> items) {
        Fragment frgElement = new Fragment();
        frgElement.setFid(String.valueOf(part.getPartId()));
        frgElement.setT(part.getPartType().getCode());
        if (part.getParentPart() != null) {
            frgElement.setPid(String.valueOf(part.getParentPart().getPartId()));
        }

        if (CollectionUtils.isNotEmpty(items)) {
            for (ApItem item : items) {
                DescriptionItem di = convertor.convert(item);
                frgElement.getDdOrDoOrDp().add(di);
            }
        }
        return frgElement;
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

        AccessPointState accessPointState = AccessPointState.NEW;
        if (apState.getStateApproval() != null) {
        	switch(apState.getStateApproval())
        	{
        	case APPROVED:
        		accessPointState = AccessPointState.APPROVED;
        		break;
        	default:
        		accessPointState = AccessPointState.NEW;
        		break;
        	}
        }

        AccessPointEntry entry = new AccessPointEntry();
        entry.setId(ap.getAccessPointId().toString());
        entry.setT(apState.getApType().getCode());
        entry.setS(accessPointState);
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

        /*
        @Override
        public APRefConvertor createAPRefConvertor() {
            return new APRefConvertor() {
                @Override
                public DescriptionItemAPRef convert(ArrData data, ObjectFactory objectFactory) {
                    DescriptionItemAPRef item = super.convert(data, objectFactory);
                    return item;
                }
            };
        }*/
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
