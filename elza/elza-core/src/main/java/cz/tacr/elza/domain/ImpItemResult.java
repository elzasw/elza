package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * One recorded outcome of an imp_item; several rows per item are possible.
 */
@Entity(name = "imp_item_result")
public class ImpItemResult {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer itemResultId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ImpItem.class)
    @JoinColumn(name = "item_id", nullable = false)
    private ImpItem item;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ImpResultType resultType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fund_id")
    private ArrFund fund;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "fund_change_id")
    private ArrChange fundChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "ap_change_id")
    private ApChange apChange;

    public Integer getItemResultId() { return itemResultId; }
    public void setItemResultId(Integer itemResultId) { this.itemResultId = itemResultId; }

    public ImpItem getItem() { return item; }
    public void setItem(ImpItem item) { this.item = item; }

    public ImpResultType getResultType() { return resultType; }
    public void setResultType(ImpResultType resultType) { this.resultType = resultType; }

    public ArrFund getFund() { return fund; }
    public void setFund(ArrFund fund) { this.fund = fund; }

    public ArrChange getFundChange() { return fundChange; }
    public void setFundChange(ArrChange fundChange) { this.fundChange = fundChange; }

    public ApChange getApChange() { return apChange; }
    public void setApChange(ApChange apChange) { this.apChange = apChange; }
}
