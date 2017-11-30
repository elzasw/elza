package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

public class InternalNode extends AbstractInternalNode {

    private final List<XmlNode> nodes = new ArrayList<>();

    public InternalNode(QName name) {
        super(name);
    }

    public InternalNode(String localName) {
        super(new QName(localName));
    }

    public void addNode(XmlNode node) {
        nodes.add(node);
    }

    @Override
    public Iterator<XmlNode> iterator() {
        return nodes.iterator();
    }

    @Override
    public void clear() {
        super.clear();
        nodes.clear();
    }
}
