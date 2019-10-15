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

/**
 * Entity for locked values
 * 
 *
 */
@Entity(name = "arr_locked_value")
@Table
public class ArrLockedValue {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db    
    Integer lockedValueId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class)
    @JoinColumn(name = "fundId", nullable = false)
    private ArrFund fund;

    @Column(name = "fundId", updatable = false, insertable = false)
    private Integer fundId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    ArrChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrItem.class)
    @JoinColumn(name = "itemId", nullable = false)
    ArrItem item;

    @Column(name = "itemId", updatable = false, insertable = false)
    Integer itemId;

    public Integer getLockedValueId() {
        return lockedValueId;
    }

    public void setLockedValueId(Integer usedValueId) {
        this.lockedValueId = usedValueId;
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

    public ArrItem getItem() {
        return item;
    }

    public void setItem(ArrItem item) {
        this.item = item;
        this.itemId = (item != null) ? item.getItemId() : null;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }
}
