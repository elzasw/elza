package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Záznam o posledním úspěšném doběhnutím hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public interface ArrBulkActionRun<FC extends ArrChange, FAV extends ArrFundVersion> extends Serializable {

    /**
     * Vrací identifikátor záznamu.
     *
     * @return identifikátor záznamu
     */
    Integer getBulkActionRunId();


    /**
     * Nastaví identifikátor záznamu.
     *
     * @param bulkActionId identifikátor záznamu
     */
    void setBulkActionRunId(Integer bulkActionId);


    /**
     * Vrací kód hromadné akce.
     *
     * @return kód hromadné akce
     */
    String getBulkActionCode();


    /**
     * Nastaví kód hromadné akce.
     *
     * @param bulkActionCode kód hromadné akce
     */
    void setBulkActionCode(String bulkActionCode);


    /**
     * Vrací verzi archivní pomůcky.
     *
     * @return verze archivní pomůcky
     */
    FAV getFundVersion();


    /**
     * Nastavuje verzi archivní pomůcky.
     *
     * @param fundVersion verze archivní pomůcky
     */
    void setFundVersion(FAV fundVersion);


    /**
     * Vrací změnu se kterou běžela hromadná akce.
     *
     * @return změna
     */
    FC getChange();


    /**
     * Nastavuje změnu se kterou běžela hromadná akce.
     *
     * @param change změna
     */
    void setChange(FC change);

}
