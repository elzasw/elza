package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.search.ItemIndexingInterceptor;


/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Indexed(interceptor = ItemIndexingInterceptor.class)
@Entity(name = "arr_desc_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrDescItem extends ArrItem {

    public static final String NODE = "node";
    public static final String CREATE_CHANGE_ID = "createChangeId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @Transient
    private final Integer fundId;
    
    /**
     * Sets fund id for index when node is only reference (detached hibernate proxy).
     */
    public ArrDescItem(Integer fundId) {
        this.fundId = fundId;
    }
    
    public ArrDescItem() {
        this((Integer) null);
    }
  
    public ArrDescItem(ArrItemData item) {
        this((Integer) null);
        this.item = item;
    }

    @Field(store = Store.YES)
    public String getDescItemIdString() {
        return getItemId().toString();
    }

    @Override
    @Field(store = Store.YES)
    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public Integer getFundId() {
        if (fundId != null) {
            return fundId;
        }
        return node.getFundId();
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return null; //throw new NotImplementedException();
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node == null ? null : node.getNodeId();
    }

    @Override
    public String toString() {
        return "ArrDescItem pk=" + getItemId();
    }
}
