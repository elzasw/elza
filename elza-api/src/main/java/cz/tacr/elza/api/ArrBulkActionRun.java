package cz.tacr.elza.api;

import cz.tacr.elza.api.vo.result.Result;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * Záznam o posledním úspěšném doběhnutím hromadné akce.
 *
 * @param <FC>   the type parameter
 * @param <FAV>  the type parameter
 * @param <ABAN> the type parameter
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public interface ArrBulkActionRun<FC extends ArrChange, FAV extends ArrFundVersion, ABAN extends ArrBulkActionNode,
        OD extends ArrOutputDefinition, R extends Result> extends Serializable {

    /**
     * Stav hromadné akce
     */
    enum State {
        /**
         * Čekající
         */
        WAITING,
        /**
         * Naplánovaný
         */
        PLANNED,
        /**
         * Běžící
         */
        RUNNING,
        /**
         * Dokončená
         */
        FINISHED,
        /**
         * Chyba při běhu
         */
        ERROR,
        /**
         * Zrušená
         */
        INTERRUPTED,
        /**
         * Neplatný
         */
        OUTDATED;
    }

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
     * Vrátí user id.
     *
     * @return user id
     */
    Integer getUserId();

    /**
     * Nastaví user id.
     *
     * @param userId user id
     */
    void setUserId(Integer userId);

    /**
     * Vrací změnu se kterou běžela hromadná akce.
     *
     * @return změna change
     */
    FC getChange();


    /**
     * Nastavuje změnu se kterou běžela hromadná akce.
     *
     * @param change změna
     */
    void setChange(FC change);

    /**
     * Vrátí stav hromadné akce
     *
     * @return stav state
     */
    State getState();

    /**
     * Nastaví stav hromadné akce
     *
     * @param state stav
     */
    void setState(State state);


    /**
     * Vrátí datum naplánování hromadné akce
     *
     * @return datum date planed
     */
    Date getDatePlanned();


    /**
     * Nastavuje datum kdy byla naplánována hromadná akce
     *
     * @param datePlaned datum
     */
    void setDatePlanned(Date datePlaned);


    /**
     * Vrátí datum startu hromadné akce
     *
     * @return datum date started
     */
    Date getDateStarted();


    /**
     * Nastavuje datum kdy byla spuštěna hromadná akce
     *
     * @param dateStarted datum
     */
    void setDateStarted(Date dateStarted);


    /**
     * Vrátí datum dokončení hromadné akce
     *
     * @return datum date finished
     */
    Date getDateFinished();


    /**
     * Nastavuje datum kdy byla dokončena hromadná akce
     *
     * @param dateFinished datum
     */
    void setDateFinished(Date dateFinished);

    /**
     * Vrátí chybu které nastala při běhu hromadné
     *
     * @return String log chyby
     */
    String getError();

    /**
     * Nastaví chybu které nastala při běhu hromadné akce
     *
     * @param error string log chyba
     */
    void setError(String error);

    /**
     * Vazba na bulk action nodes
     *
     * @param arrBulkActionNodes množina záznamů.
     */
    void setArrBulkActionNodes(List<ABAN> arrBulkActionNodes);

    /**
     * Vazba na bulk action nodes
     *
     * @return množina, může být prázdná.
     */
    List<ABAN> getArrBulkActionNodes();

    /**
     * @return výsledek hromadné akce
     */
    R getResult();

    /**
     * @param result výsledek hromadné akce
     */
    void setResult(R result);

    /**
     * @return vazba na výstup
     */
    OD getOutputDefinition();

    /**
     * @param outputDefinition vazba na výstup
     */
    void setOutputDefinition(OD outputDefinition);

}
