package cz.tacr.elza.domain;

import java.util.Objects;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;


/**
 * Data jednotky popisu serializované pro rychlejší sestavení.
 *
 */
@Table
@Entity(name = "arr_cached_node")
public class ArrCachedNode {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer cachedNodeId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
	@JoinColumn(name = "nodeId", nullable = false)
	private ArrNode node;

	@Column(insertable = false, updatable = false)
    private Integer nodeId;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column
    private String data;

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
