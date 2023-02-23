package cz.tacr.elza.domain;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DataRepositoryImpl;
import cz.tacr.elza.search.DescItemIndexingInterceptor;

/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 */
//@AnalyzerDef(name = "customanalyzer",
//        tokenizer = @TokenizerDef(factory = KeywordTokenizerFactory.class),
//        filters = {
//                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
//        })
@Indexed(interceptor = DescItemIndexingInterceptor.class)
@Entity(name = "arr_desc_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrDescItem extends ArrItem {

    public static final String TABLE_NAME = "arr_desc_item";

	// Constants for fulltext indexing
    public static final String FIELD_ITEM_ID = "itemId";
    public static final String FIELD_FUND_ID = "fundId";
    public static final String FIELD_FUND_ID_STRING = "fundIdString";
    public static final String FIELD_NODE = "node";
    public static final String FIELD_NODE_ID = "nodeId";
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
    @GenericField(name = FIELD_NODE_ID,  projectable = Projectable.YES)
    // @FieldBridge(impl = IntegerBridge.class)
    public Integer getNodeId() {
        return nodeId;
    }

    @JsonIgnore
    @Override
    @GenericField(name = FIELD_FUND_ID,   projectable = Projectable.YES)
    @Facet(name = FIELD_FUND_ID_STRING, forField = FIELD_FUND_ID, encoding = FacetEncodingType.STRING)
    // @FieldBridge(impl = IntegerBridge.class)
    public Integer getFundId() {
        return indexData.getFundId();
    }

	@JsonIgnore
	@FullTextField(name = FULLTEXT_ATT)
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
	@GenericField(name = INTGER_ATT, projectable = Projectable.YES)
    public Integer getValueInt() {
        return indexData.getValueInt();
    }

	@JsonIgnore
	@GenericField(name = DECIMAL_ATT, projectable = Projectable.YES)
    public Double getValueDouble() {
        return indexData.getValueDouble();
    }

    @JsonIgnore
    @GenericField(name = DATE_ATT, projectable = Projectable.YES)
    public Date getValueDate() {
        return indexData.getValueDate();
    }

    @JsonIgnore
    @GenericField(name = BOOLEAN_ATT, projectable = Projectable.YES)
    public Boolean isValue() {
        return indexData.isValue();
    }

	@JsonIgnore
	@GenericField(name = NORMALIZED_FROM_ATT, projectable = Projectable.YES)
    public Long getNormalizedFrom() {
        return indexData.getNormalizedFrom();
    }

	@JsonIgnore
	@GenericField(name = NORMALIZED_TO_ATT, projectable = Projectable.YES)
    public Long getNormalizedTo() {
        return indexData.getNormalizedTo();
    }

	/**
	 * Description item for fulltext indexing
	 *
	 * @return
	 */
	@JsonIgnore
    @GenericField(name = FIELD_DESC_ITEM_TYPE_ID)
	//@FieldBridge(impl = IntegerBridge.class)
	public Integer getDescItemTypeId() {
		return itemTypeId;
	}

	@JsonIgnore
	@GenericField(name = SPECIFICATION_ATT)
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
