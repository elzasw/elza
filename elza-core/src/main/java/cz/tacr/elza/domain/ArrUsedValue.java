package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Entity for used values
 * 
 *
 */
@Entity(name = "arr_used_value")
@Table
public class ArrUsedValue {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db    
    Integer usedValueId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(name = "fundId", updatable = false, insertable = false)
    private Integer fundId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    RulItemType itemType;

    @Column(name = "itemTypeId", updatable = false, insertable = false)
    Integer itemTypeId;

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    String value;

    public Integer getUsedValueId() {
        return usedValueId;
    }

    public void setUsedValueId(Integer usedValueId) {
        this.usedValueId = usedValueId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
        fundId = (fund != null) ? fund.getFundId() : null;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(Integer fundId) {
        this.fundId = fundId;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ArrChange createChange) {
        this.createChange = createChange;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(RulItemType itemType) {
        this.itemType = itemType;
        this.itemTypeId = (itemType != null) ? itemType.getItemTypeId() : null;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
