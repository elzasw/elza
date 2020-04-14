package cz.tacr.elza.dataexchange.output.writer.xml;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.aps.ApInfo;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.ExternalId;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * XML output stream for access points export.
 */
public class XmlApOutputStream extends BaseFragmentStream implements ApOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(AccessPoint.class);

    private final RootNode rootNode;

    public XmlApOutputStream(RootNode rootNode, Path tempDirectory) {
        super(tempDirectory);
        this.rootNode = rootNode;
    }

    @Override
    public void addAccessPoint(ApInfo apInfo) {
        Validate.isTrue(!isProcessed());

        AccessPoint element = new AccessPoint();
        element.setApe(createEntry(apInfo.getApState(), apInfo.getExternalIds()));

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

    public static AccessPointEntry createEntry(ApState apState, Collection<ApExternalId> eids) {
        ApAccessPoint ap = apState.getAccessPoint();

        AccessPointEntry entry = new AccessPointEntry();
        entry.setId(ap.getAccessPointId().toString());
        entry.setT(apState.getApType().getCode());
        entry.setUuid(ap.getUuid());

        // prepare external id
        if (CollectionUtils.isNotEmpty(eids)) {
            List<ExternalId> elementList = entry.getEid();
            for (ApExternalId eid : eids) {
                ExternalId element = new ExternalId();
                element.setV(eid.getValue());
                element.setT(eid.getExternalIdType().getCode());
                elementList.add(element);
            }
        }
        return entry;
    }
}
