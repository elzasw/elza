package cz.tacr.elza.print;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

/**
 * Implementace datového zdroje pro iterující uzly.
 */
public class JRDataIterableNodes implements JRDataSource {

    /**
     * Iterátor uzlů, které procházíme.
     */
    private final IteratorNodes iteratorNodes;

    /**
     * Poslední vrácený uzel.
     */
    private Node actual;

    public JRDataIterableNodes(final IteratorNodes iteratorNodes) {
        this.iteratorNodes = iteratorNodes;
    }

    @Override
    public boolean next() throws JRException {
        boolean hasNext = iteratorNodes.hasNext();
        if (hasNext) {
            actual = iteratorNodes.next();
        }
        return hasNext;
    }

    @Override
    public Object getFieldValue(final JRField jrField) throws JRException {
        String name = jrField.getName();
        switch (name) {
            case "depth":
                return actual.getDepth();
            case "node":
                return actual;
            default:
                throw new IllegalStateException("Uknown field, name: " + name);
        }
    }
}
