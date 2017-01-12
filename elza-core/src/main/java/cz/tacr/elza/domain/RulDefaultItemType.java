package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Vazební tabulka mezi pravidlem a typem atribut - určení atributů, které jsou implicitní pro zobrazení, využívá se pro klilenta.
 *
 * @author Pavel Stánek
 * @since 10.06.2016
 */
@Entity(name = "rul_default_item_type")
@Table
public class RulDefaultItemType {
    @Id
    @GeneratedValue
    private Integer defaultItemTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    public Integer getDefaultItemTypeId() {
        return defaultItemTypeId;
    }

    public void setDefaultItemTypeId(final Integer defaultItemTypeId) {
        this.defaultItemTypeId = defaultItemTypeId;
    }

    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;

    }

    public RulItemType getItemType() {
        return itemType;
    }
}
