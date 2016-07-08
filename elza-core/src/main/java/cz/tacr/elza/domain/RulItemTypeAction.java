package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Implementace {@link cz.tacr.elza.api.RulItemTypeAction}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "rul_item_type_action")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemTypeAction implements cz.tacr.elza.api.RulItemTypeAction<RulAction, RulItemType> {

    @Id
    @GeneratedValue
    private Integer itemTypeActionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulAction.class)
    @JoinColumn(name = "actionId", nullable = false)
    private RulAction action;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @Override
    public Integer getItemTypeActionId() {
        return itemTypeActionId;
    }

    @Override
    public void setItemTypeActionId(final Integer itemTypeActionId) {
        this.itemTypeActionId = itemTypeActionId;
    }

    @Override
    public RulAction getAction() {
        return action;
    }

    @Override
    public void setAction(final RulAction action) {
        this.action = action;
    }

    @Override
    public RulItemType getItemType() {
        return itemType;
    }

    @Override
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }
}
