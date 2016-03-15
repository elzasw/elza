package cz.tacr.elza.api.controller;

import java.util.List;

import cz.tacr.elza.api.vo.BulkActionConfig;
import cz.tacr.elza.api.vo.BulkActionState;


/**
 * Rozhraní pro obsluhu hromadných akcí.
 *
 * @param <BAC> {@link BulkActionConfig} hromadná akce
 * @param <BAS> {@link BulkActionState} stav hromadné akce
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public interface BulkActionManager<BAC extends BulkActionConfig, BAS extends BulkActionState> {

    /**
     * Vrací seznam typů hromadných akcí, které jsou v systému zaregistrované.
     *
     * @return seznam typů hromadných akcí
     */
    List<String> getBulkActionTypes();


    /**
     * Vytvoření hromadné akce.
     *
     * @param bulkActionConfig konfigurace hromadné akce
     * @return vytvořená konfigurace hromadná akce
     */
    BAC createBulkAction(BAC bulkActionConfig);


    /**
     * Změna hromadné akce.
     *
     * @param bulkActionConfig konfigurace hromadné akce
     * @return upravená konfigurace hromadná akce
     */
    BAC updateBulkAction(BAC bulkActionConfig);


    /**
     * Vrací konfiguraci hromadné akce.
     *
     * @param bulkActionCode kód hromadné akce
     * @return hromadná akce
     */
    BAC getBulkAction(String bulkActionCode);


    /**
     * Vrací seznam stavů hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam stavů hromadných akcí
     */
    List<BAS> getBulkActionState(Integer fundVersionId);


    /**
     * Smaže konfiguraci hromadné akce.
     *
     * @param bulkActionConfig konfigurace hromadné akce
     */
    void deleteBulkAction(BAC bulkActionConfig);


    /**
     * Provede přenačtení konfigurací hromadných akcí - nutné pouze pokud byly upravovány v konfiguračních souborech.
     */
    void reload();


    /**
     * Vrací seznam všech konfigurací hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     */
    List<BAC> getBulkActions(Integer fundVersionId);


    /**
     * Vrací seznam povinných konfigurací hromadných akcí podle verze archivní pomůcky.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     */
    List<BAC> getMandatoryBulkActions(Integer fundVersionId);


    /**
     * Spustí hromadnou akci ve zvolené verze archivní pomůcce.
     *
     * @param bulkActionConfig    konfigurace hromadné akce
     * @param fundVersionId identifikátor verze archivní pomůcky
     */
    void run(BAC bulkActionConfig, Integer fundVersionId);


    /**
     * Spustí validaci verze AP.
     *
     * @param fundVersionId identifikátor verze archivní pomůcky
     * @return seznam konfigurací hromadných akcí, které je nutné ještě spustit před uzavřením verze
     */
    List<BAC> runValidation(Integer fundVersionId);

}
