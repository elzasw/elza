package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * popis {@link cz.tacr.elza.api.RulDescItemType}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_desc_item_type")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"}),
        @UniqueConstraint(columnNames = {"viewOrder"})})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulDescItemType implements cz.tacr.elza.api.RulDescItemType<RulDataType> {

    @Id
    @GeneratedValue
    private Integer descItemTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDataType.class)
    @JoinColumn(name = "dataTypeId", nullable = false)
    private RulDataType dataType;

    @Column(nullable = false)
    private Boolean sys;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String shortcut;

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;

    @Column(nullable = false)
    private Boolean isValueUnique;

    @Column(nullable = false)
    private Boolean canBeOrdered;

    @Column(nullable = false)
    private Boolean useSpecification;

    @Column(nullable = false)
    private Integer viewOrder;


    @Override
    public Integer getDescItemTypeId() {
        return descItemTypeId;
    }

    @Override
    public void setDescItemTypeId(final Integer descItemTypeId) {
        this.descItemTypeId = descItemTypeId;
    }

    @Override
    public RulDataType getDataType() {
        return dataType;
    }

    @Override
    public void setDataType(final RulDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public Boolean getSys() {
        return sys;
    }

    @Override
    public void setSys(final Boolean sys) {
        this.sys = sys;
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
    public String getShortcut() {
        return shortcut;
    }

    @Override
    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
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
    public Boolean getIsValueUnique() {
        return isValueUnique;
    }

    @Override
    public void setIsValueUnique(final Boolean isValueUnique) {
        this.isValueUnique = isValueUnique;
    }

    @Override
    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    @Override
    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    @Override
    public Boolean getUseSpecification() {
        return useSpecification;
    }

    @Override
    public void setUseSpecification(final Boolean useSpecification) {
        this.useSpecification = useSpecification;
    }

    @Override
    public Integer getViewOrder() {
        return viewOrder;
    }

    @Override
    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulDescItemType)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulDescItemType other = (RulDescItemType) obj;

        return new EqualsBuilder().append(descItemTypeId, other.getDescItemTypeId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descItemTypeId).append(name).append(code).toHashCode();
    }
}
