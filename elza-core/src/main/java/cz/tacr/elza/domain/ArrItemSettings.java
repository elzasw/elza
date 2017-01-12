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
 * Nastavení pro atribut výstupu.
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

    /**
     * @return identifikátor entity
     */
    public Integer getItemSettingsId() {
        return itemSettingsId;
    }

    /**
     * @param itemSettingsId identifikátor entity
     */
    public void setItemSettingsId(final Integer itemSettingsId) {
        this.itemSettingsId = itemSettingsId;
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

    /**
     * @return výstup
     */
    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    /**
     * @param outputDefinition výstup
     */
    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    /**
     * @return výsledek
     */
    public Boolean getBlockActionResult() {
        return blockActionResult;
    }

    /**
     * @param blockActionResult výsledek
     */
    public void setBlockActionResult(final Boolean blockActionResult) {
        this.blockActionResult = blockActionResult;
    }
}
