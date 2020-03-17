package cz.tacr.elza.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.FacetEncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.repository.DataRepositoryImpl;
import cz.tacr.elza.search.DescItemIndexingInterceptor;


/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 */
@AnalyzerDef(name = "customanalyzer",
        tokenizer = @TokenizerDef(factory = KeywordTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
        })
@Indexed(interceptor = DescItemIndexingInterceptor.class)
@Entity(name = "arr_desc_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrDescItem extends ArrItem {

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
		this.indexData = src.indexData;
		this.node = src.node;
		this.nodeId = src.nodeId;
	}

    @Override
    @Field(name = FIELD_NODE_ID, analyze = Analyze.NO, store = Store.YES)
    // @FieldBridge(impl = IntegerBridge.class)
    public Integer getNodeId() {
        return nodeId;
    }

    @JsonIgnore
    @Override
    @Field(name = FIELD_FUND_ID, analyze = Analyze.NO, store = Store.YES)
    @Facet(name = FIELD_FUND_ID_STRING, forField = FIELD_FUND_ID, encoding = FacetEncodingType.STRING)
    // @FieldBridge(impl = IntegerBridge.class)
    public Integer getFundId() {
        return indexData.getFundId();
    }

	@JsonIgnore
	@Field(name = FULLTEXT_ATT)
    @Analyzer(definition = "customanalyzer")
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
	@Field(name = INTGER_ATT, store = Store.YES)
    @NumericField
    public Integer getValueInt() {
        return indexData.getValueInt();
    }

	@JsonIgnore
	@Field(name = DECIMAL_ATT, store = Store.YES)
    @NumericField
    public Double getValueDouble() {
        return indexData.getValueDouble();
    }

    @JsonIgnore
    @Field(name = DATE_ATT, store = Store.YES)
    @NumericField
    public Date getValueDate() {
        return indexData.getValueDate();
    }

    @JsonIgnore
    @Field(name = BOOLEAN_ATT, store = Store.YES)
    public Boolean isValue() {
        return indexData.isValue();
    }

	@JsonIgnore
	@Field(name = NORMALIZED_FROM_ATT, store = Store.YES)
    @NumericField
    public Long getNormalizedFrom() {
        return indexData.getNormalizedFrom();
    }

	@JsonIgnore
	@Field(name = NORMALIZED_TO_ATT, store = Store.YES)
    @NumericField
    public Long getNormalizedTo() {
        return indexData.getNormalizedTo();
    }

	/**
	 * Description item for fulltext indexing
	 *
	 * @return
	 */
	@JsonIgnore
    @Field(name = FIELD_DESC_ITEM_TYPE_ID)
	//@FieldBridge(impl = IntegerBridge.class)
	public Integer getDescItemTypeId() {
		return itemTypeId;
	}

	@JsonIgnore
	@Field(name = SPECIFICATION_ATT)
	@Analyzer(definition = "customanalyzer")
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
