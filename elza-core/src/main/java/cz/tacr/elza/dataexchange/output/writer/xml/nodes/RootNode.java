package cz.tacr.elza.dataexchange.output.writer.xml.nodes;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.dataexchange.output.writer.xml.XmlNameConsts;

public class RootNode extends AbstractInternalNode {

    public enum ChildNodeType {

        ACCESS_POINTS, PARTIES, INSTITUTIONS, RELATIONS, SECTIONS;
    }

    private final XmlNode[] childNodes = new XmlNode[ChildNodeType.values().length];

    public RootNode() {
        super(XmlNameConsts.ROOT);
    }

    public XmlNode getNode(ChildNodeType type) {
        return childNodes[type.ordinal()];
    }

    public void setNode(ChildNodeType type, XmlNode node) {
        Validate.isTrue(childNodes[type.ordinal()] == null);
        Validate.notNull(node);

        childNodes[type.ordinal()] = node;
    }

    @Override
    public Iterator<XmlNode> iterator() {
        return new ArrayNotNullElementIterator<>(childNodes);
    }

    @Override
    public void clear() {
        super.clear();
        Arrays.fill(childNodes, null);
    }

    @Override
    protected void writeBeforeChilds(XMLStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartDocument();
        super.writeBeforeChilds(streamWriter);
    }

    @Override
    protected void writeAfterChilds(XMLStreamWriter streamWriter) throws XMLStreamException {
        super.writeAfterChilds(streamWriter);
        streamWriter.writeEndDocument();
    }

    public static class ArrayNotNullElementIterator<T> implements Iterator<T> {

        private final T[] elements;

        private final int lastIndex;

        private int index = -1;

        public ArrayNotNullElementIterator(T[] elements) {
            this.elements = Validate.notNull(elements);
            this.lastIndex = getLastNotNullIndex(elements);
        }

        @Override
        public T next() {
            if (index < lastIndex) {
                for (int i = index + 1; i <= lastIndex; i++) {
                    T element = elements[i];
                    if (element != null) {
                        index = i;
                        return element;
                    }
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasNext() {
            return index < lastIndex;
        }

        private static int getLastNotNullIndex(Object[] elements) {
            for (int i = elements.length - 1; i >= 0; i--) {
                if (elements[i] != null) {
                    return i;
                }
            }
            return -1;
        }
    }
}
