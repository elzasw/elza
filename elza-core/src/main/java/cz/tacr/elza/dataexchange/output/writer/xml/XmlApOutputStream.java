package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.writer.ApInfo;
import cz.tacr.elza.dataexchange.output.writer.ApOutputStream;
import cz.tacr.elza.dataexchange.output.writer.BaseApInfo;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointName;
import cz.tacr.elza.schema.v2.AccessPointtNames;
import cz.tacr.elza.schema.v2.ExternalId;

/**
 * XML output stream for access points export.
 */
public class XmlApOutputStream implements ApOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(AccessPoint.class);

    private final RootNode rootNode;

    private final XmlFragment fragment;

    private boolean processed;

    public XmlApOutputStream(RootNode rootNode, Path tempDirectory) {
        this.rootNode = rootNode;
        this.fragment = new XmlFragment(tempDirectory);
    }

    @Override
    public void addAccessPoint(ApInfo apInfo) {
        Validate.isTrue(!processed);

        AccessPoint element = new AccessPoint();
        element.setApe(createEntry(apInfo));
        element.setChr(apInfo.getDesc().getDescription());
        element.setNms(createNames(apInfo.getNames()));

        try {
            writeAP(element);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Override
    public void processed() {
        Validate.isTrue(!processed);

        try {
            fragment.close();
        } catch (XMLStreamException | IOException e) {
            throw new SystemException(e);
        }

        if (fragment.isExist()) {
            FileNode node = new FileNode(fragment.getPath());
            rootNode.setNode(ChildNodeType.ACCESS_POINTS, node);
        }
        processed = true;
    }

    @Override
    public void close() {
        if (processed) {
            return;
        }
        try {
            fragment.delete();
        } catch (IOException e) {
            throw new SystemException(e);
        }
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

    public static AccessPointEntry createEntry(BaseApInfo apInfo) {
        ApAccessPoint ap = apInfo.getAp();
        AccessPointEntry entry = new AccessPointEntry();
        entry.setId(ap.getAccessPointId().toString());
        entry.setT(ap.getApType().getCode());
        entry.setUuid(ap.getUuid());

        // prepare external id
        Collection<ApExternalId> eids = apInfo.getExternalIds();
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

    private static AccessPointtNames createNames(Collection<ApName> names) {
        if (CollectionUtils.isEmpty(names)) {
            return null;
        }
        AccessPointtNames listElement = new AccessPointtNames();
        List<AccessPointName> list = listElement.getNm();

        for (ApName name : names) {
            AccessPointName element = new AccessPointName();
            element.setCpl(name.getComplement());
            element.setL(name.getLanguage());
            element.setN(name.getName());
            element.setT(name.getNameType().getCode());
            list.add(element);
        }
        return listElement;
    }
}
