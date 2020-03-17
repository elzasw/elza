package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
 * Evidence možných datových typů atributů archivního popisu.
 * Evidence je společná pro všechny archivní pomůcky.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_data_type")
@Table
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDataType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    @Column
    private Integer textLengthLimit;

    public Integer getDataTypeId() {
        return dataTypeId;
    }

    public void setDataTypeId(final Integer dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * @return příznak, zda je možná u datového typu kontrola na regulární výraz.
     */
    public Boolean getRegexpUse() {
        return regexpUse;
    }

    /**
     * @param regexpUse příznak, zda je možná u datového typu kontrola na regulární výraz.
     */
    public void setRegexpUse(final Boolean regexpUse) {
        this.regexpUse = regexpUse;
    }

    /**
     * @return příznak, zda je možná u datového typu kontrola na maximální možnou délku textového řetězce.
     */
    public Boolean getTextLengthLimitUse() {
        return textLengthLimitUse;
    }

    /**
     * @param textLengthLimitUse příznak, zda je možná u datového typu kontrola na maximální možnou délku textového řetězce.
     */
    public void setTextLengthLimitUse(final Boolean textLengthLimitUse) {
        this.textLengthLimitUse = textLengthLimitUse;
    }

    /**
     * @return informace, kde je ulozena hodnota (arr_data_xxx).
     */
    public String getStorageTable() {
        return storageTable;
    }

    /**
     * @param storageTable informace, kde je ulozena hodnota (arr_data_xxx).
     */
    public void setStorageTable(final String storageTable) {
        this.storageTable = storageTable;
    }


    public Integer getTextLengthLimit() {
        return textLengthLimit;
    }

    public void setTextLengthLimit(Integer textLengthLimit) {
        this.textLengthLimit = textLengthLimit;
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
