package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * evidence možných datových typů atributů archivního popisu.
 * evidence je společná pro všechny archivní pomůcky.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_data_type")
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDataType implements cz.tacr.elza.api.RulDataType {

    @Id
    @GeneratedValue
    private Integer dataTypeId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(nullable = false)
    private Boolean regexpUse;

    @Column(nullable = false)
    private Boolean textLengthLimitUse;

    @Column(length = 250, nullable = false)
    private String storageTable;

    @Override
    public Integer getDataTypeId() {
        return dataTypeId;
    }

    @Override
    public void setDataTypeId(final Integer dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public Boolean getRegexpUse() {
        return regexpUse;
    }

    @Override
    public void setRegexpUse(final Boolean regexpUse) {
        this.regexpUse = regexpUse;
    }

    @Override
    public Boolean getTextLengthLimitUse() {
        return textLengthLimitUse;
    }

    @Override
    public void setTextLengthLimitUse(final Boolean textLengthLimitUse) {
        this.textLengthLimitUse = textLengthLimitUse;
    }

    @Override
    public String getStorageTable() {
        return storageTable;
    }

    @Override
    public void setStorageTable(final String storageTable) {
        this.storageTable = storageTable;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulDataType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulDataType other = (RulDataType) obj;

        return new EqualsBuilder().append(dataTypeId, other.getDataTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dataTypeId).append(name).append(code).toHashCode();
    }

    @Override
    public String toString() {
        return "RulDataType pk=" + dataTypeId;
    }
}
