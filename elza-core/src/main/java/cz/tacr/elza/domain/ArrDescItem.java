package cz.tacr.elza.domain;

import java.util.Date;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.bridge.ArrDescItemBinder;
import cz.tacr.elza.domain.bridge.ArrDescItemRoutingBinder;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataRepositoryImpl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 */
@Table
@Indexed//(routingBinder = @RoutingBinderRef(type = ArrDescItemRoutingBinder.class))
@TypeBinding(binder = @TypeBinderRef(type = ArrDescItemBinder.class))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@Entity(name = "arr_desc_item")
public class ArrDescItem extends ArrItem {

    public static final String TABLE_NAME = "arr_desc_item";

	// Constants for fulltext indexing
    public static final String FIELD_ITEM_ID = "itemId";
    public static final String FIELD_FUND_ID = "fundId";
    public static final String FIELD_FUND_ID_STRING = "fundIdString";
    public static final String FIELD_NODE = "node";
    public static final String FIELD_NODE_ID = "nodeId";
    public static final String FIELD_CREATE_CHANGE = "createChange";
    public static final String FIELD_DELETE_CHANGE = "deleteChange";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_DESC_ITEM_TYPE_ID = "descItemTypeId";
	public static final String FULLTEXT_ATT = "fulltextValue";
	public static final String SPECIFICATION_ATT = "specification";
	public static final String INTGER_ATT = "valueInt";
	public static final String DECIMAL_ATT = "valueDecimal";
	public static final String DATE_ATT = "valueDate";
	public static final String BOOLEAN_ATT = "valueBoolean";
	public static final String NORMALIZED_FROM_ATT = "normalizedFrom";
	public static final String NORMALIZED_TO_ATT = "normalizedTo";

	@JsonIgnore
    @RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
	@JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @Transient
    private final ArrDescItemIndexData indexData;

    /**
     * Externalized Lucene index data.
     */
    public ArrDescItem(ArrDescItemIndexData indexData) {
        this.indexData = indexData;
    }

    public ArrDescItem() {
        this.indexData = new SimpleIndexData();
    }

	/**
	 * Copy constructor
	 *
	 * @param src Source object
	 */
	public ArrDescItem(ArrDescItem src) {
        super(src);
        if (src.indexData != null && (src.indexData instanceof SimpleIndexData)) {
            // set own instance of SimpleIndexData
            // instance cannot be shared, each object needs its own
            this.indexData = new SimpleIndexData();
        } else {
            this.indexData = src.indexData;
        }
		this.node = src.node;
		this.nodeId = src.nodeId;
	}

    @Override
    public Integer getNodeId() {
        return nodeId;
    }

    @JsonIgnore
    @Override
    public Integer getFundId() {
        return indexData.getFundId();
    }

	@JsonIgnore
    public String getFulltextValue() {
		if (data == null) {
            return null;
		}
		if (data instanceof ArrDataNull) {
            return itemSpec == null ? null : itemSpec.getName();
        }
        String fulltext = indexData.getFulltextValue();
        return itemSpec == null ? fulltext : itemSpec.getName() + DataRepositoryImpl.SPEC_SEPARATOR + fulltext;
    }

	@JsonIgnore
    public Integer getValueInt() {
        return indexData.getValueInt();
    }

	@JsonIgnore
    public Double getValueDouble() {
        return indexData.getValueDouble();
    }

    @JsonIgnore
    public Date getValueDate() {
        return indexData.getValueDate();
    }

    @JsonIgnore
    public Boolean isValue() {
        return indexData.isValue();
    }

	@JsonIgnore
    public Long getNormalizedFrom() {
        return indexData.getNormalizedFrom();
    }

	@JsonIgnore
    public Long getNormalizedTo() {
        return indexData.getNormalizedTo();
    }

	/**
	 * Description item for fulltext indexing
	 *
	 * @return
	 */
	@JsonIgnore
	public Integer getDescItemTypeId() {
		return itemTypeId;
	}

	@JsonIgnore
	public Integer getSpecification() {
		return itemSpecId;
	}

    @Override
    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node == null ? null : node.getNodeId();
    }

    @Override
    public ArrOutput getOutput() {
        return null; //throw new NotImplementedException();
    }

    @Override
    public String toString() {
        return "ArrDescItem pk=" + getItemId();
    }

    private class SimpleIndexData implements ArrDescItemIndexData {

        @Override
        public Integer getFundId() {
            if (node == null) {
                throw new BusinessException("Valid node not set", BaseCode.SYSTEM_ERROR);
            }
            return node.getFundId();
        }

        @Override
        public String getFulltextValue() {
			return (data == null) ? null : data.getFulltextValue();
        }

        @Override
        public Integer getValueInt() {
			return (data == null) ? null : data.getValueInt();
        }

        @Override
        public Double getValueDouble() {
			return (data == null) ? null : data.getValueDouble();
        }

        @Override
        public Long getNormalizedFrom() {
			return (data == null) ? null : data.getNormalizedFrom();
        }

        @Override
		public Long getNormalizedTo() {
			return (data == null) ? null : data.getNormalizedTo();
        }

        @Override
        public Date getValueDate() {
            return (data == null) ? null : data.getDate();
        }

        @Override
        public Boolean isValue() { return (data == null) ? null : data.getValueBoolean();}
    }

    @Override
    public ArrItem makeCopy() {
        return new ArrDescItem(this);
    }
}
