package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Implementace {@link cz.tacr.elza.api.ArrItemSettings}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "arr_item_settings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrItemSettings {

    @Id
    @GeneratedValue
    private Integer itemSettingsId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    @Column(nullable = false)
    private Boolean blockActionResult;

    public Integer getItemSettingsId() {
        return itemSettingsId;
    }

    public void setItemSettingsId(final Integer itemSettingsId) {
        this.itemSettingsId = itemSettingsId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }

    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    public Boolean getBlockActionResult() {
        return blockActionResult;
    }

    public void setBlockActionResult(final Boolean blockActionResult) {
        this.blockActionResult = blockActionResult;
    }
}
