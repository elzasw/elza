package cz.tacr.elza.dataexchange.output.writer.xml;

import cz.tacr.elza.common.XmlUtils;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.FileNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode;
import cz.tacr.elza.dataexchange.output.writer.xml.nodes.RootNode.ChildNodeType;
import cz.tacr.elza.schema.v2.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamWriter;
import java.nio.file.Path;

/**
 * XML output stream for parties export.
 */
public class XmlPartiesOutputStream extends BaseFragmentStream {

    private final static Logger logger = LoggerFactory.getLogger(XmlPartiesOutputStream.class);

    private final JAXBContext jaxbContext = XmlUtils.createJAXBContext(Person.class, PartyGroup.class, Family.class, Event.class);

    private final RootNode rootNode;

    public XmlPartiesOutputStream(RootNode rootNode, Path tempDirectory) {
        super(tempDirectory);
        this.rootNode = rootNode;
    }


    public void processed() {
        finishFragment();

        if (fragment.isExist()) {
            FileNode node = new FileNode(fragment.getPath());
            rootNode.setNode(ChildNodeType.PARTIES, node);
        }
    }

    public void close() {
        closeFragment();
    }

    private void writeParty(Party party) throws Exception {
        if (!fragment.isOpen()) {
            XMLStreamWriter sw = fragment.openStreamWriter();
            sw.writeStartDocument();
            sw.writeStartElement(XmlNameConsts.PARTIES);
        }

        XMLStreamWriter sw = fragment.getStreamWriter();
        String partyName = XmlNameConsts.getPartyName(party);
        JAXBElement<?> jaxbElement = XmlUtils.wrapElement(partyName, party);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, sw);
    }

}
