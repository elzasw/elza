package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.Assert;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class NodeId implements RecordProvider, Comparable<NodeId> {

    private final Output output; // vazba na parent output
    private final int arrNodeId; // vazba na DB objekt, povinný údaj

    private Integer parentNodeId = null; // vazba na parentNode
    private Set<NodeId> childNodeIds = new TreeSet<>((o1, o2) -> (
            Integer.valueOf(o1.getPosition()).compareTo(Integer.valueOf(o2.getPosition())))); // vazba na child nodes
    private int position;
    private int depth;

    /**
     * Konstruktor s povinnými hodnotami
     * @param output vazba na output
     * @param arrNodeId ID definice DB uzlu, z něhož se vychází
     * @param parentNodeId ID parent Node
     * @param depth hloubka uzlu od kořene
     * @param position pozice uzlu
     */
    public NodeId(final Output output, final int arrNodeId, final Integer parentNodeId, final int position,
            final int depth) {
        Assert.notNull(position);
        Assert.notNull(depth);

        this.output = output;
        this.arrNodeId = arrNodeId;
        this.parentNodeId = parentNodeId;
        this.position = position;
        this.depth = depth;
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo neexistuje vrací null.
     */
    public NodeId getParent() {
        if (parentNodeId == null) {
            return null;
        }
        return output.getNodeId(parentNodeId);
    }

    public void setParentNodeId(final Integer parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    /**
     * @return vrací seznam dětí, omezeno jen na node v outputu
     */
    public Set<NodeId> getChildren() {
        return childNodeIds;
    }

    /**
     * Metoda pro získání skutečné hodnoty do fieldu.
     * Umožní na položce v detailu volat metody sám nad sebou (nejen implicitně zpřístupněné gettery).
     *
     * @return odkaz sám na sebe
     */
    public Node getNode() {
        try {
            return output.getNodeCache().get(this.arrNodeId);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    public int getArrNodeId() {
        return arrNodeId;
    }

    public Integer getDepth() {
        return depth;
    }

    public Integer getPosition() {
        return position;
    }

    @Override
    public List<Record> getRecords() {
        return getNode().getRecords();
    }

    @Override
    public List<NodeId> getRecordProviderChildren() {
        return new ArrayList<>(getChildren());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof NodeId) {
            final NodeId o1 = (NodeId) o;
            return new EqualsBuilder().append(arrNodeId, o1.getArrNodeId()).isEquals();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // podstatný je zdrojový arrNode
        return new HashCodeBuilder().append(arrNodeId).toHashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ").
                add(Integer.toString(depth)).
                add(Integer.toString(position)).
                toString();
    }

    @Override
    public int compareTo(final NodeId o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
