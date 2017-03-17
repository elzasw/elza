package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.bridge.builtin.IntegerBridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Tabulka pro evidenci hodnot atributů archivního popisu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@AnalyzerDef(name = "customanalyzer",
tokenizer = @TokenizerDef(factory = KeywordTokenizerFactory.class),
filters = {
  @TokenFilterDef(factory = LowerCaseFilterFactory.class),
})
@Entity(name = "arr_data")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class ArrData {

    public static final String ITEM = "item";

    public static final String LUCENE_DESC_ITEM_TYPE_ID = "descItemTypeId";

    @Id
    @GeneratedValue
    private Integer dataId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrItem.class)
    @JoinColumn(name = "itemId", nullable = true)
    private ArrItem item;

    /** @return vrací hodnotu pro fulltextové hledání  */
    @Field
    @Analyzer(definition = "customanalyzer")
    public abstract String getFulltextValue();

    @Field(store = Store.YES)
    public String getItemId() {
        return item.getItemId().toString();
    }

    @Field(store = Store.YES)
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getNodeId() {
        return item.getNode() != null ? item.getNode().getNodeId() : null;
    }

    @Field
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getFundId() {
        return item.getFundId();
    }

    @Field(store = Store.NO)
    @FieldBridge(impl = IntegerBridge.class)
    public Integer getDescItemTypeId() {
        return item.getItemType().getItemTypeId();
    }

    @Field
    @Analyzer(definition = "customanalyzer")
    public Integer getSpecification() {
        RulItemSpec descItemSpec = item.getItemSpec();
        if (descItemSpec == null) {
            return null;
        }

        return descItemSpec.getItemSpecId();
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(final Integer dataId) {
        this.dataId = dataId;
    }

    public RulDataType getDataType() {
        return dataType;
    }

    public void setDataType(final RulDataType dataType) {
        this.dataType = dataType;
    }

    public ArrItem getItem() {
        return item;
    }

    public void setItem(final ArrItem item) {
        this.item = item;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrData)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ArrData other = (ArrData) obj;

        return new EqualsBuilder().append(dataId, other.getDataId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dataId).toHashCode();
    }

    @Override
    public String toString() {
        return "ArrData pk=" + dataId;
    }
}
