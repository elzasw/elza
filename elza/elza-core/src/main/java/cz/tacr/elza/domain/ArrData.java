package cz.tacr.elza.domain;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.service.cache.NodeCacheSerializable;


/**
 * Tabulka pro evidenci hodnot atributů archivního popisu.
 */
@Entity(name = "arr_data")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrData implements NodeCacheSerializable {

    public static final String ID = "dataId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer dataId;

    @JsonIgnore
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer dataTypeId;

	/**
	 * Default constructor
	 *
	 */
	protected ArrData() {

	}

	/**
	 * Copy constructor
	 *
	 * @param src
	 */
	protected ArrData(ArrData src) {
		this.dataId = src.dataId;

		this.dataType = src.getDataType();
		this.dataTypeId = src.getDataTypeId();

        // If we are copying from existing item then dataType have to be set
		Validate.notNull(dataType);
		Validate.notNull(dataTypeId);
	}

	public Integer getDataId() {
        return dataId;
    }

    public void setDataId(final Integer dataId) {
        this.dataId = dataId;
    }

    public RulDataType getDataType() {
	    if (dataType == null && dataTypeId != null) {
	        dataType = DataType.fromId(dataTypeId).getEntity();
        }
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
    @JsonIgnore
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

    @JsonIgnore
    @Transient
    public Date getDate() {
        return null;
    }

    @JsonIgnore
    @Transient
    public Boolean getValueBoolean() {
        return null;
    }

    /**
     * Compare values
     *
     * Method is comparing only values without IDs
     *
     * @param srcData
     * @return
     */
    public boolean isEqualValue(ArrData srcData) {
        // we can compare only real objects
        srcData = HibernateUtils.unproxyInitialized(srcData);

        // check data type
        Validate.isTrue(srcData.dataTypeId==this.dataTypeId);

        return isEqualValueInternal(srcData);
    }

    /**
     * Internal implementation of value comparator
     *
     * @param srcData
     * @return
     */
    abstract protected boolean isEqualValueInternal(ArrData srcData);

	/**
	 * Prepare copy of the data object
	 *
	 * Method returns pure data copy of the source object without saving it to
	 * the DB
	 *
	 * @return Return copy of the object
	 */
	abstract public ArrData makeCopy();

    /**
     * Merge data from source object
     *
     * Method is called from merge method and
     * data type is already checked.
     */
    abstract protected void mergeInternal(final ArrData srcData);

    /**
     * Merge values from source.
     *
     * dataType of source have to match dataType of this object
     *
     * @param srcData
     *            Source object
     */
    public void merge(final ArrData srcData) {
        mergeInternal(srcData);
    }

	/**
	 * Make copy of the source data object and set id to null
	 *
	 * Method returns pure data copy of the source object without saving it to
	 * the DB
	 *
	 * @param src
	 *            Source object
	 * @return Return copy of the data object
	 */
	public static ArrData makeCopyWithoutId(ArrData src) {
		ArrData trg = src.makeCopy();
		trg.setDataId(null);
		return trg;
	}

    /**
     * Validate data state
     *
     * Function check if object contains minimum required
     * information to be save in DB.
     *
     * Method throws RuntimeException if problem is found.
     */
    public void validate() {
        Validate.notNull(this.dataTypeId);
        validateInternal();
    }

    /**
     * Each domain object has to implement its own validation
     */
    abstract protected void validateInternal();
}
