package cz.tacr.elza.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rul_item_type_spec_assign")
public class RulItemTypeSpecAssign {
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    @Column(name = "item_type_spec_assign_id", nullable = false)
    private Integer itemTypeSpecAssignId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_type_id", referencedColumnName = "item_type_id", nullable = false)
    private RulItemType itemType;

   /* @Column(updatable = false, insertable = false)
    private Integer itemTypeId;*/

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_spec_id", referencedColumnName = "itemSpecId", nullable = false)
    private RulItemSpec itemSpec;

   /*@Column(updatable = false, insertable = false)
    private Integer itemSpecId;*/

    @Column(name="item_spec_view_order", nullable = false)
    private Integer viewOrder;

    protected RulItemTypeSpecAssign() {
    }

    public RulItemTypeSpecAssign(final RulItemType itemType, final RulItemSpec itemSpec, final Integer viewOrder) {
        this.itemType = itemType;
        this.itemSpec = itemSpec;
        this.viewOrder = viewOrder;
    }

    public Integer getItemTypeSpecAssignId() {
        return itemTypeSpecAssignId;
    }

    public void setItemTypeSpecAssignId(Integer itemTypeSpecAssignId) {
        this.itemTypeSpecAssignId = itemTypeSpecAssignId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(RulItemType itemType) {
       // this.itemTypeId = itemType != null ? itemType.getItemTypeId() : null;
        this.itemType = itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(RulItemSpec itemSpec) {
       // this.itemSpecId = itemSpec != null ? itemSpec.getItemSpecId() : null;
        this.itemSpec = itemSpec;
    }

   /* public Integer getItemTypeId() {
        return itemTypeId;
    }

    public Integer getItemSpecId() {
        return itemSpecId;
    }*/

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(itemTypeSpecAssignId).append(itemType.getItemTypeId()).append(itemSpec.getItemSpecId()).toHashCode();
    }

    @Override
    public String toString() {
        return "RulItemTypeSpecAssign pk=" + itemTypeSpecAssignId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.RulItemTypeSpecAssign)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.RulItemTypeSpecAssign other = (cz.tacr.elza.domain.RulItemTypeSpecAssign) obj;

        return new EqualsBuilder().append(itemTypeSpecAssignId, other.getItemTypeSpecAssignId()).isEquals();
    }

    /**
     * @return pořadí zobrazení.
     */
    public Integer getViewOrder() {
        return viewOrder;
    }

    /**
     * @param viewOrder pořadí zobrazení.
     */
    public void setViewOrder(final Integer viewOrder) {
        this.viewOrder = viewOrder;
    }


}
