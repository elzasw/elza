package cz.tacr.elza.print;

import cz.tacr.elza.service.output.OutputFactoryService;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
@Scope("prototype")
public class NodeId implements RecordProvider, Comparable<NodeId> {
    private final Output output; // vazba na parent output
    private final Integer arrNodeId; // vazba na DB objekt, povinný údaj
    private final Integer arrLevelId; // vazba na DB objekt, povinný údaj

    private Integer parentNodeId = null; // vazba na parentNode
    private Set<NodeId> childNodeIds = new HashSet<>(); // vazba na child nodes
    private Integer position;
    private Integer depth;

    @Autowired
    private OutputFactoryService outputFactoryService;

    /**
     * Konstruktor s povinnými hodnotami
     * @param output vazba na output
     * @param arrNodeId ID definice DB uzlu, z něhož se vychází
     * @param arrLevelId ID definice DB uzlu, z něhož se vychází
     * @param parentNodeId ID parent Node
     */
    public NodeId(Output output, Integer arrNodeId, Integer arrLevelId, Integer parentNodeId) {
        this.output = output;
        this.arrNodeId = arrNodeId;
        this.arrLevelId = arrLevelId;
        this.parentNodeId = parentNodeId;
    }

    /**
     * @return dohledá v output.modes node, který je nadřazený tomuto. Pokud není nalezen nebo neexistuje vrací null.
     */
    public NodeId getParent() {
        return output.getNodesMap().get(parentNodeId);
    }

    public void setParentNodeId(Integer parentNodeId) {
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

    public Integer getArrNodeId() {
        return arrNodeId;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public List<Record> getRecords() {
        return getNode().getRecords();
    }

    @Override
    public List<NodeId> getRecordProviderChildern() {
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
        return new StringJoiner(", ").add(depth.toString()).add(position.toString()).toString();
    }

    @Override
    public int compareTo(NodeId o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }
}
