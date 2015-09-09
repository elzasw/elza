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
 * Evidence možných specifikací typů atributů archivního popisu. Evidence je společná pro všechny
 * archivní pomůcky. Vazba výčtu specifikací na různá pravidla bude řešeno později. Podtyp atributu
 * (Role entit - Malíř, Role entit - Sochař, Role entit - Spisovatel).
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "rul_desc_item_spec")
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"}),
        @UniqueConstraint(columnNames = {"viewOrder"})})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulDescItemSpec implements cz.tacr.elza.api.RulDescItemSpec<RulDescItemType, RegRegisterType> {

    @Id
    @GeneratedValue
    private Integer descItemSpecId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "descItemTypeId", nullable = false)
    private RulDescItemType descItemType;

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
    private Integer viewOrder;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = true)
    private RegRegisterType registerType;

    @Override
    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    @Override
    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }

    @Override
    public RulDescItemType getDescItemType() {
        return descItemType;
    }

    @Override
    public void setDescItemType(final RulDescItemType descItemType) {
        this.descItemType = descItemType;
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
    public Integer getViewOrder() {
        return viewOrder;
    }

    @Override
    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    @Override
    public RegRegisterType getRegisterType() {
        return registerType;
    }


    @Override
    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulDescItemSpec)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulDescItemSpec other = (RulDescItemSpec) obj;

        return new EqualsBuilder().append(descItemSpecId, other.getDescItemSpecId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(descItemSpecId).append(name).append(code).toHashCode();
    }

}
