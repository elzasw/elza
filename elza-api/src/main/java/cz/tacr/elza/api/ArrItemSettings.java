package cz.tacr.elza.api;

/**
 * Nastavení pro atribut výstupu.
 *
 * @author Martin Šlapa
 * @since 27.06.2016
 */
public interface ArrItemSettings<IT extends RulItemType, OD extends ArrOutputDefinition> {

    /**
     * @return identifikátor entity
     */
    Integer getItemSettingsId();

    /**
     * @param itemSettingsId identifikátor entity
     */
    void setItemSettingsId(Integer itemSettingsId);

    /**
     * @return typ atributu
     */
    IT getItemType();

    /**
     * @param itemType typ atributu
     */
    void setItemType(IT itemType);

    /**
     * @return výstup
     */
    OD getOutputDefinition();

    /**
     * @param outputDefinition výstup
     */
    void setOutputDefinition(OD outputDefinition);

    /**
     * @return výsledek
     */
    Boolean getBlockActionResult();

    /**
     * @param blockActionResult výsledek
     */
    void setBlockActionResult(Boolean blockActionResult);
}
