package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;

import javax.persistence.*;


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


    public Integer getDescItemTypeId() {
        return descItemTypeId;
    }

    public void setDescItemTypeId(final Integer descItemTypeId) {
        this.descItemTypeId = descItemTypeId;
    }

    public RulDataType getDataType() {
        return dataType;
    }

    public void setDataType(final RulDataType dataType) {
        this.dataType = dataType;
    }

    public Boolean getSys() {
        return sys;
    }

    public void setSys(final Boolean sys) {
        this.sys = sys;
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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(final String shortcut) {
        this.shortcut = shortcut;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getIsValueUnique() {
        return isValueUnique;
    }

    public void setIsValueUnique(final Boolean isValueUnique) {
        this.isValueUnique = isValueUnique;
    }

    public Boolean getCanBeOrdered() {
        return canBeOrdered;
    }

    public void setCanBeOrdered(final Boolean canBeOrdered) {
        this.canBeOrdered = canBeOrdered;
    }

    public Boolean getUseSpecification() {
        return useSpecification;
    }

    public void setUseSpecification(final Boolean useSpecification) {
        this.useSpecification = useSpecification;
    }

    public Integer getViewOrder() {
        return viewOrder;
    }

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
