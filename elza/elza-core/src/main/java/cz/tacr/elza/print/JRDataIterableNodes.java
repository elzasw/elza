package cz.tacr.elza.print;

// Waiting from JasperReports 7
// import net.sf.jasperreports.engine.JRDataSource;
// import net.sf.jasperreports.engine.JRException;
// import net.sf.jasperreports.engine.JRField;

/**
 * Implementace datového zdroje pro iterující uzly.
 */
public class JRDataIterableNodes
// implements JRDataSource 
{
    //
    //    /**
    //     * Iterátor uzlů, které procházíme.
    //     */
    //    private final NodeIterator nodeIterator;
    //
    //    /**
    //     * Poslední vrácený uzel.
    //     */
    //    private Node actual;
    //
    //    public JRDataIterableNodes(final NodeIterator nodeIterator) {
    //        this.nodeIterator = nodeIterator;
    //    }
    //
    //    @Override
    //    public boolean next() throws JRException {
    //        boolean hasNext = nodeIterator.hasNext();
    //        if (hasNext) {
    //            actual = nodeIterator.next();
    //        }
    //        return hasNext;
    //    }
    //
    //    @Override
    //    public Object getFieldValue(final JRField jrField) throws JRException {
    //        String name = jrField.getName();
    //        switch (name) {
    //            case "depth":
    //                return actual.getDepth();
    //            case "node":
    //                return actual;
    //            default:
    //                throw new IllegalStateException("Uknown field, name: " + name);
    //        }
    //    }
}
