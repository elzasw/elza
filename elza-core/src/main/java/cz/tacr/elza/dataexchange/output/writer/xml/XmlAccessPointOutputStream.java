package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.output.writer.AccessPointsOutputStream;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPointVariantNames;
import cz.tacr.elza.schema.v2.ExternalId;
import cz.tacr.elza.utils.XmlUtils;

/**
 * XML output stream for access points export.
 */
public class XmlAccessPointOutputStream implements AccessPointsOutputStream {

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(AccessPoint.class);

    private final RootNode rootNode;

    private final XmlFragment fragment;

    private boolean processed;

    public XmlAccessPointOutputStream(RootNode rootNode, Path tempDirectory) {
        this.rootNode = rootNode;
        this.fragment = new XmlFragment(tempDirectory);
    }

    @Override
    public void addAccessPoint(RegRecord accessPoint) {
        Validate.isTrue(!processed);

        AccessPoint element = new AccessPoint();
        element.setApe(createEntry(accessPoint));
        element.setChr(accessPoint.getCharacteristics());
        element.setN(accessPoint.getRecord());
        element.setNote(accessPoint.getNote());
        element.setVnms(createVariantNames(accessPoint));

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
            sw.writeStartElement(XmlElementName.ACCESS_POINTS);
        }
        XMLStreamWriter sw = fragment.getStreamWriter();
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(XmlElementName.PARTY, ap);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, sw);
    }

    public static AccessPointEntry createEntry(RegRecord ap) {
        AccessPointEntry entry = new AccessPointEntry();
        entry.setId(ap.getRecordId().toString());
        entry.setT(ap.getRegisterType().getCode());
        entry.setUpd(XmlUtils.convertDate(ap.getLastUpdate()));
        entry.setUuid(ap.getUuid());

        if (ap.getParentRecordId() != null) {
            entry.setPid(ap.getParentRecordId().toString());
        }
		if (StringUtils.isNotBlank(ap.getExternalId())) {
            ExternalId eid = new ExternalId();
            eid.setId(ap.getExternalId());
            eid.setEsc(ap.getExternalSystem().getCode());
            entry.setEid(eid);
        }

        return entry;
    }

    private static AccessPointVariantNames createVariantNames(RegRecord ap) {
        List<RegVariantRecord> variantNames = ap.getVariantRecordList();
        if (variantNames == null || variantNames.isEmpty()) {
            return null;
        }
        AccessPointVariantNames listElement = new AccessPointVariantNames();
        List<String> list = listElement.getVnm();

        variantNames.forEach(source -> list.add(source.getRecord()));

        return listElement;
    }
}
