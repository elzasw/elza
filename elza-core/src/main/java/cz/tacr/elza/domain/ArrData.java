package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.service.cache.NodeCacheSerializable;


/**
 * Tabulka pro evidenci hodnot atributů archivního popisu.
 */
@Entity(name = "arr_data")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrData implements NodeCacheSerializable {

    public static final String ID = "dataId";

    @Id
    @GeneratedValue
    private Integer dataId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer dataTypeId;

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
        this.dataTypeId = dataType != null ? dataType.getDataTypeId() : null;
    }

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    // TODO: consider to remove getDataType() and setDataType(...), rename this method to getDataType()
    public DataType getType() {
        if (dataTypeId == null) {
            return null;
        }
        return DataType.fromId(dataTypeId);
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

    /* methods for data indexing */

    @JsonIgnore
    @Transient
    public abstract String getFulltextValue();

    @JsonIgnore
    @Transient
    public Integer getValueInt() {
        return null;
    }

    @JsonIgnore
    @Transient
    public Double getValueDouble() {
        return null;
    }

    @JsonIgnore
    @Transient
    public Long getNormalizedFrom() {
        return null;
    }

    @JsonIgnore
    @Transient
    public Long getNormalizedTo() {
        return null;
    }
}
