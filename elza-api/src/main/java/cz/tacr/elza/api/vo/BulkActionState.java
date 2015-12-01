package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.ArrChange;


/**
 * Stav instance hromadné akce.
 *
 * @author Martin Šlapa
 * @since 10.11.2015
 */
public interface BulkActionState<FC extends ArrChange> {

    /**
     * Typy stavů, ve kterém může hromadná akce být.
     */
    enum State {
        WAITING,
        // čeká ve frontě pro verzi archivní pomůcky
        PLANNED,
        // je naplánován, čeká na náplánování
        RUNNING,
        // běží
        FINISH,
        // doběhnul úspěšně
        ERROR    // doběhnul s chybou
    }


    /**
     * Vrací změnu, se kterou byla hromadná akce spuštěna.
     *
     * @return změna
     */
    FC getRunChange();


    /**
     * Nastavení změny, se kterou byla hromadná akce spuštěna.
     *
     * @param runChange změna
     */
    void setRunChange(FC runChange);


    /**
     * Vrací stav hromadné akce.
     *
     * @return stav
     */
    State getState();


    /**
     * Nastavení stavu hromadné akce.
     *
     * @param state stav
     */
    void setState(State state);


    /**
     * Vrací identifikátor procesu hromadné akce.
     *
     * @return identifikátor procesu hromadné akce
     */
    Integer getProcessId();


    /**
     * Nastavení identifikátor procesu hromadné akce.
     *
     * @param processId identifikátor procesu hromadné akce
     */
    void setProcessId(Integer processId);


    /**
     * Vrací kód hromadné akce.
     *
     * @return kód hromadné akce
     */
    String getBulkActionCode();


    /**
     * Nastavení kódu hromadné akce.
     *
     * @param bulkActionCode kód hromadné akce
     */
    void setBulkActionCode(String bulkActionCode);

}
