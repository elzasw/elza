package cz.tacr.elza.domain;

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

import cz.tacr.elza.api.UIPartyGroupTypeEnum;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Nastavení zobrazení formuláře pro osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 25. 10. 2016
 */
@Entity(name = "ui_party_group")
public class UIPartyGroup implements cz.tacr.elza.api.UIPartyGroup<ParPartyType> {

    @Id
    @GeneratedValue
    private Integer partyGroupId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ParPartyType.class)
    @JoinColumn(name = "partyTypeId", nullable = false)
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

    @Override
	public Integer getPartyGroupId() {
        return partyGroupId;
    }

    @Override
	public void setPartyGroupId(final Integer partyGroupId) {
        this.partyGroupId = partyGroupId;
    }

    @Override
	public ParPartyType getPartyType() {
        return partyType;
    }

    @Override
	public void setPartyType(final ParPartyType partyType) {
        this.partyType = partyType;
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
	public Integer getViewOrder() {
        return viewOrder;
    }

    @Override
	public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }

    @Override
	public UIPartyGroupTypeEnum getType() {
        return type;
    }

    @Override
	public void setType(final UIPartyGroupTypeEnum type) {
        this.type = type;
    }

    @Override
	public String getContentDefinition() {
        return contentDefinition;
    }

    @Override
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
}
