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
public interface ArrOutputDefinition<F extends ArrFund,N extends ArrNodeOutput,O extends ArrOutput,R extends RulOutputType, T extends RulTemplate> extends Serializable {

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

    List<O> getOutputs();

    void setOutputs(List<O> outputs);

    List<N> getOutputNodes();

    void setOutputNodes(List<N> outputNodes);

    R getOutputType();

    void setOutputType(R outputType);

    T getTemplate();
    
    void setTemplate(T template);
}
