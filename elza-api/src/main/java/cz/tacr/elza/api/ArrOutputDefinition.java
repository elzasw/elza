package cz.tacr.elza.api;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Výstup z archivního souboru.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
public interface ArrOutputDefinition<F extends ArrFund,N extends ArrNodeOutput,O extends ArrOutput,R extends RulOutputType, T extends RulTemplate, Q extends ArrOutputResult> extends Serializable {

    /**
     * Stav outputu
     */
    enum OutputState {
        /**
         * Rozpracovaný
         */
        OPEN,
        /**
         * Běží hromadná akce
         */
        COMPUTING,
        /**
         * Generování
         */
        GENERATING,
        /**
         * Vygenerovaný
         */
        FINISHED,
        /**
         * Vygenerovaný neaktuální
         */
        OUTDATED
    }

    /**
     * @return identifikátor entity
     */
    Integer getOutputDefinitionId();

    /**
     * @param outputDefinitionId identifikátor entity
     */
    void setOutputDefinitionId(Integer outputDefinitionId);

    LocalDateTime getLastUpdate();

    void setLastUpdate(LocalDateTime lastUpdate);

    /**
     * @return archivní soubor
     */
    F getFund();

    /**
     * @param fund archivní soubor
     */
    void setFund(F fund);

    /**
     * @return kód výstupu
     */
    String getInternalCode();

    /**
     * @param internalCode kód výstupu
     */
    void setInternalCode(String internalCode);

    /**
     * @return jméno výstupu
     */
    String getName();

    /**
     * @param name jméno výstupu
     */
    void setName(String name);

    /**
     * @return příznak dočasného výstupu, lze použít pro Adhoc výstupy
     */
    Boolean getTemporary();

    /**
     * @param temporary příznak dočasného výstupu, lze použít pro Adhoc výstupy
     */
    void setTemporary(Boolean temporary);

    /**
     * @return příznak, že byl archivní fond smazán
     */
    Boolean getDeleted();

    /**
     * @param deleted příznak, že byl archivní fond smazán
     */
    void setDeleted(Boolean deleted);

    /**
     * @return List verzí outputů
     */
    List<O> getOutputs();

    /**
     * @param outputs List verzí outputů
     */
    void setOutputs(List<O> outputs);

    /**
     * @return List nodů outputu
     */
    List<N> getOutputNodes();

    /**
     * @param outputNodes List nodů outputu
     */
    void setOutputNodes(List<N> outputNodes);

    /**
     * @return Typ outputu
     */
    R getOutputType();

    /**
     * @param outputType Typ outputu
     */
    void setOutputType(R outputType);

    /**
     * @return šablona outputu
     */
    T getTemplate();

    /**
     * @param template šablona outputu
     */
    void setTemplate(T template);

    /**
     * @return Výsledek outputu
     */
    Q getOutputResult();

    /**
     * @param outputResult Výsledek outputu
     */
    void setOutputResult(Q outputResult);

    /**
     * @return stav
     */
    OutputState getState();

    /**
     * @param state stav
     */
    void setState(OutputState state);

    /**
     * @return Vrátí chybu outputu
     */
    String getError();

    /**
     * @param error nastaví chybu outputu
     */
    void setError(String error);
}
