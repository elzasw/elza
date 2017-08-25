package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.search.DescItemIndexingInterceptor;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.builtin.IntegerBridge;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Atribut archivního popisu evidovaný k jednotce archivního popisu. Odkaz na uzel stromu AP je
 * řešen pomocí node_id.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
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

    public static final String NODE = "node";
    public static final String CREATE_CHANGE_ID = "createChangeId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";
    public static final String LUCENE_DESC_ITEM_TYPE_ID = "descItemTypeId";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    @JsonIgnore
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    public ArrDescItem() {

    }

    @Field(store = Store.YES)
    public String getDescItemIdString() {
        return getItemId().toString();
    }

    @Override
    @Field(store = Store.YES)
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getNodeId() {
        return nodeId;
    }

    @Field
    @Analyzer(definition = "customanalyzer")
    public String getFulltextValue() {
        ArrData data = getData();
        if (data == null) {
            return null;
        } else {
            if (data instanceof ArrDataNull) {
                RulItemSpec itemSpec = getItemSpec();
                return itemSpec == null ? null : itemSpec.getName();
            } else {
                return data.getFulltextValue();
            }
        }
    }

    @Field(store = Store.NO)
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getDescItemTypeId() {
        return getItemType().getItemTypeId();
    }

    @Field
    @Analyzer(definition = "customanalyzer")
    public Integer getSpecification() {
        RulItemSpec itemSpec = getItemSpec();
        if (itemSpec == null) {
            return null;
        }
        return itemSpec.getItemSpecId();
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    @Field
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getFundId() {
        return node.getFundId();
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @IndexedEmbedded
    @Override
    public ArrData getData() {
        return super.getData();
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

    public static String concatDataAttribute(final String attribute) {
        return ArrItem.DATA + "." + attribute;
    }
}
