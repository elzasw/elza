package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.api.enums.UIPartyGroupTypeEnum;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Nastavení zobrazení formuláře pro osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 25. 10. 2016
 */
@Entity(name = "ui_party_group")
public class UIPartyGroup {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer partyGroupId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId")
    private ParPartyType partyType;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer viewOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UIPartyGroupTypeEnum type;

    @Column
    @Lob
    @org.hibernate.annotations.Type(type = "org.hibernate.type.TextType")
    private String contentDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getPartyGroupId() {
        return partyGroupId;
    }

    public void setPartyGroupId(final Integer partyGroupId) {
        this.partyGroupId = partyGroupId;
    }

    public ParPartyType getPartyType() {
        return partyType;
    }

    public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
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

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    public UIPartyGroupTypeEnum getType() {
        return type;
    }

    public void setType(final UIPartyGroupTypeEnum type) {
        this.type = type;
    }

    public String getContentDefinition() {
        return contentDefinition;
    }

    public void setContentDefinition(final String contentDefinition) {
        this.contentDefinition = contentDefinition;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof UIPartyGroup)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        UIPartyGroup other = (UIPartyGroup) obj;

        return new EqualsBuilder().append(partyGroupId, other.getPartyGroupId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(partyGroupId).toHashCode();
    }

    @Override
    public String toString() {
        return "UIPartyGroup pk=" + partyGroupId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
