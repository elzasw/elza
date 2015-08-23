package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_data_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDataType implements IdObject<Integer>, cz.tacr.elza.api.RulDataType {

    @Id
    @GeneratedValue
    private Integer dataTypeId;

    @Column(length = 15, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 15, nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(nullable = false)
    private Boolean regexpUse;

    @Column(nullable = false)
    private Boolean textLenghtLimitUse;

    @Column(length = 200, nullable = false)
    private String storageTable;

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

    public Boolean getRegexpUse() {
        return regexpUse;
    }

    public void setRegexpUse(final Boolean regexpUse) {
        this.regexpUse = regexpUse;
    }

    public Boolean getTextLenghtLimitUse() {
        return textLenghtLimitUse;
    }

    public void setTextLenghtLimitUse(final Boolean textLenghtLimitUse) {
        this.textLenghtLimitUse = textLenghtLimitUse;
    }

    public String getStorageTable() {
        return storageTable;
    }

    public void setStorageTable(final String storageTable) {
        this.storageTable = storageTable;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return dataTypeId;
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

        return new EqualsBuilder().append(getId(), other.getId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getId()).append(name).append(code).toHashCode();
    }

}
