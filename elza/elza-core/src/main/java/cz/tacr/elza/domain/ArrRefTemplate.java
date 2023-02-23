package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "arr_ref_template")
public class ArrRefTemplate {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer refTemplateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemNodeRefId")
    private RulItemType itemNodeRef;

    public Integer getRefTemplateId() {
        return refTemplateId;
    }

    public void setRefTemplateId(Integer refTemplateId) {
        this.refTemplateId = refTemplateId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RulItemType getItemNodeRef() {
        return itemNodeRef;
    }

    public void setItemNodeRef(RulItemType itemNodeRef) {
        this.itemNodeRef = itemNodeRef;
    }
}
