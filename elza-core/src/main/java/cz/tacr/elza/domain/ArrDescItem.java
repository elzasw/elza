package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.search.DescItemIndexingInterceptor;


/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Indexed(interceptor = DescItemIndexingInterceptor.class)
@Entity(name = "arr_desc_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrDescItem implements cz.tacr.elza.api.ArrDescItem<ArrChange, RulItemType, RulItemSpec, ArrNode> {

    public static final String NODE = "node";
    public static final String CREATE_CHANGE_ID = "createChangeId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";
    public static final String ITEM_SPEC = "itemSpec";
    public static final String ITEM_TYPE = "itemType";

    @Id
    @GeneratedValue
    private Integer descItemId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Column(nullable = false)
    private Integer descItemObjectId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = true)
    private RulItemSpec itemSpec;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(nullable = false)
    private Integer position;

    @Field(store = Store.YES)
    public String getDescItemIdString() {
        return descItemId.toString();
    }

    @Field(store = Store.YES)
    public Integer getNodeId() {
        return node.getNodeId();
    }

    @Field
    @NumericField
    public Integer getCreateChangeId() {
        return createChangeId;
    }

    @Field
    @NumericField
    public Integer getDeleteChangeId() {
        return deleteChangeId == null ? Integer.MAX_VALUE : deleteChangeId;
    }

    @Override
    public Integer getDescItemId() {
        return descItemId;
    }

    @Override
    public void setDescItemId(final Integer descItemId) {
        this.descItemId = descItemId;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
        if (deleteChange != null) {
            deleteChangeId = deleteChange.getChangeId();
        }
    }

    @Override
    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    @Override
    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrDescItem)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (getDescItemId() == null) {
            return false;
        }

        ArrDescItem other = (ArrDescItem) obj;

        return new EqualsBuilder().append(descItemId, other.getDescItemId()).isEquals();
    }

    @Override
    public String toString() {
        return "ArrDescItem pk=" + descItemId;
    }
}
