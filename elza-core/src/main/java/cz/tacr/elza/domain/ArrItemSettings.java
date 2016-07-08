package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * Implementace {@link cz.tacr.elza.api.ArrItemSettings}
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
@Entity(name = "arr_item_settings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrItemSettings implements cz.tacr.elza.api.ArrItemSettings<RulItemType, ArrOutputDefinition> {

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

    @Override
    public Integer getItemSettingsId() {
        return itemSettingsId;
    }

    @Override
    public void setItemSettingsId(final Integer itemSettingsId) {
        this.itemSettingsId = itemSettingsId;
    }

    @Override
    public RulItemType getItemType() {
        return itemType;
    }

    @Override
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }

    @Override
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    @Override
    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    @Override
    public Boolean getBlockActionResult() {
        return blockActionResult;
    }

    @Override
    public void setBlockActionResult(final Boolean blockActionResult) {
        this.blockActionResult = blockActionResult;
    }
}
