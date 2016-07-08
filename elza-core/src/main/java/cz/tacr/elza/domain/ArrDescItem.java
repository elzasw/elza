package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.IntegerBridge;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrDescItem<T extends ArrItemData> extends ArrItem<T> implements cz.tacr.elza.api.ArrDescItem<ArrNode> {

    public static final String NODE = "node";
    public static final String CREATE_CHANGE_ID = "createChangeId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    public ArrDescItem() {

    }

    public ArrDescItem(final Class<T> clazz) throws IllegalAccessException, InstantiationException {
        super(clazz);
    }

    public ArrDescItem(final T item) {
        this.item = item;
    }

    @Field(store = Store.YES)
    public String getDescItemIdString() {
        return getItemId().toString();
    }

    @Override
    @Field(store = Store.YES)
    public Integer getNodeId() {
        return node.getNodeId();
    }

    @Override
    public Integer getFundId() {
        return node.getFund().getFundId();
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return null; //throw new NotImplementedException();
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "ArrDescItem pk=" + getItemId();
    }
}
