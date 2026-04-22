package cz.tacr.elza.domain;

import java.util.Objects;

import org.hibernate.Length;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

import cz.tacr.elza.domain.bridge.ArrCachedNodeBinder;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Serialized description-unit (JP) data, stored for fast assembly.
 *
 * <p>
 * Row-existence invariant: an {@code arr_cached_node} row exists for a node
 * <i>iff</i> that node has at least one {@code arr_level} with
 * {@code deleteChange IS NULL}. Invalid nodes (all levels soft-deleted) have
 * no cache row and are therefore absent from the Lucene index. See
 * {@link cz.tacr.elza.service.cache.NodeCacheService} for the full cache
 * contract and lifecycle.
 * </p>
 *
 * <p>
 * Cache consistency: the Hibernate Search index for this entity depends solely
 * on the serialized {@link #data} column (see {@link ArrCachedNodeBinder}).
 * Changes to underlying entities (ArrDescItem, ArrData, ArrNodeConformity, etc.)
 * do NOT automatically trigger reindexing — the index only refreshes when
 * {@code data} is rewritten via
 * {@link cz.tacr.elza.service.cache.NodeCacheService#syncNodes}/{@code saveNodes},
 * or when the row is deleted via
 * {@link cz.tacr.elza.service.cache.NodeCacheService#deleteNodes}. Modifying
 * node data outside those paths will leave the index stale.
 * </p>
 */
@Table
@Indexed
@TypeBinding(binder = @TypeBinderRef(type = ArrCachedNodeBinder.class))
@Entity(name = "arr_cached_node")
public class ArrCachedNode {

    // Constants for fulltext indexing
    public static final String DATA = "data";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer cachedNodeId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
	@JoinColumn(name = "nodeId", nullable = false)
	private ArrNode node;

	@Column(insertable = false, updatable = false)
    private Integer nodeId;

    @Basic
    @Column(length = Length.LONG) // hibernate long text field
    private String data;
    
    // This field does not trigger reindexing on nodeConformity change
    // Reason is uknown
    /*    @Transient
    @GenericField
	@IndexingDependency(derivedFrom = @ObjectPath({
		@PropertyValue(propertyName = "node"),
		@PropertyValue(propertyName = "nodeConformity")
		})
	)
    public Integer getNodeConformity() {
    	return null;
    }*/

    public Integer getCachedNodeId() {
        return cachedNodeId;
    }

    public void setCachedNodeId(final Integer cachedNodeId) {
        this.cachedNodeId = cachedNodeId;
    }

	public ArrNode getNode() {
        return node;
	}

	public void setNode(final ArrNode node) {
        this.node = node;
        if (node != null) {
            nodeId = node.getNodeId();
        }
	}

    public Integer getNodeId() {
        return nodeId;
    }

	/*
	protected void setNodeId(final Integer nodeId) {
	    this.nodeId = nodeId;
	}
	*/

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrCachedNode that = (ArrCachedNode) o;
        return Objects.equals(cachedNodeId, that.cachedNodeId) &&
                //Objects.equals(node, that.node) &&
                Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cachedNodeId, /*node, */nodeId, data);
    }

    @Override
    public String toString() {
        return "ArrCachedNode{" +
                "cachedNodeId=" + cachedNodeId +
                //", node=" + node +
                ", nodeId=" + nodeId +
                ", data='" + data + '\'' +
                '}';
    }
}
