package cz.tacr.elza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer itemTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputId", nullable = false)
    private ArrOutput output;

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
        this.itemTypeId = itemType != null ? itemType.getItemTypeId() : null;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    /**
     * @return výstup
     */
    public ArrOutput getOutput() {
        return output;
    }

    /**
     * @param output výstup
     */
    public void setOutput(final ArrOutput output) {
        this.output = output;
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
