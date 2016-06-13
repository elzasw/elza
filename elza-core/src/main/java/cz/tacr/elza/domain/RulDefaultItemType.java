package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Implementace RulDefaultItemType.
 *
 * @author Pavel Stánek
 * @since 10.06.2016
 */
@Entity(name = "rul_default_item_type")
@Table
public class RulDefaultItemType implements cz.tacr.elza.api.RulDefaultItemType<RulRuleSet, RulDescItemType> {
    @Id
    @GeneratedValue
    private Integer defaultItemTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulDescItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulDescItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setItemType(final RulDescItemType itemType) {
        this.itemType = itemType;

    }

    @Override
    public RulDescItemType getItemType() {
        return itemType;
    }
}
