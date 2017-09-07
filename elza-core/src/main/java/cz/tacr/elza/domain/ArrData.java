package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import cz.tacr.elza.service.cache.NodeCacheSerializable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;


/**
 * Tabulka pro evidenci hodnot atributů archivního popisu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "arr_data")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrData implements NodeCacheSerializable {

    public static String ID = "dataId";

    @Id
    @GeneratedValue
    private Integer dataId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    /**
     * @return vrací hodnotu pro fulltextové hledání
     */
    @JsonIgnore
    @Field
    @Analyzer(definition = "customanalyzer")
    public abstract String getFulltextValue();

    @JsonIgnore
    @Field(name = LuceneDescItemCondition.INTGER_ATT, store = Store.YES)
    @NumericField
    public Integer getValueInt() {
        return null;
    }

    @JsonIgnore
    @Field(name = LuceneDescItemCondition.DECIMAL_ATT, store = Store.YES)
    @NumericField
    public Double getValueDouble() {
        return null;
    }

    @JsonIgnore
    @Field(name = LuceneDescItemCondition.NORMALIZED_FROM_ATT, store = Store.YES)
    @NumericField
    public Long getNormalizedFrom() {
        return null;
    }

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, targetEntity = ArrItem.class, mappedBy = ArrItem.DATA)
    private List<ArrItem> items;

    @JsonIgnore
    @Field(name = LuceneDescItemCondition.NORMALIZED_TO_ATT, store = Store.YES)
    @NumericField
    public Long getNormalizedTo() {
        return null;
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

    public List<ArrItem> getItems() {
        return items;
    }

    public void setItems(final List<ArrItem> items) {
        this.items = items;
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
