package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vazba: Hromadná akce, která počítá hodnotu atributu výstupu.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "rul_item_type_action")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulItemTypeAction {

    @Id
    @GeneratedValue
    private Integer itemTypeActionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulAction.class)
    @JoinColumn(name = "actionId", nullable = false)
    private RulAction action;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    /**
     * @return identifikátor entity
     */
    public Integer getItemTypeActionId() {
        return itemTypeActionId;
    }

    /**
     * @param itemTypeActionId identifikátor entity
     */
    public void setItemTypeActionId(final Integer itemTypeActionId) {
        this.itemTypeActionId = itemTypeActionId;
    }

    /**
     * @return hromadná akce
     */
    public RulAction getAction() {
        return action;
    }

    /**
     * @param action hromadná akce
     */
    public void setAction(final RulAction action) {
        this.action = action;
    }

    /**
     * @return typ atributu
     */
    public RulItemType getItemType() {
        return itemType;
    }

    /**
     * @param itemType typ atributu
     */
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }
}
