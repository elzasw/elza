package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Typ výstupu podle zvolených pravidel tvorby. V případě základních pravidel se jedná o manipulační
 * seznam, inventář, katalog. Typ výstupu se používá pro kontrolu struktury archivního popisu. Je
 * realizována pouze entita obalující, nikoli další objekty, které realizují kontroly.
 * @author vavrejn
 *
 */
public interface RulArrangementType extends Serializable {
    
    Integer getArrangementTypeId();

    void setArrangementTypeId(Integer arrangementTypeId);

    /**
     * 
     * @return název typu výstupu.
     */
    String getName();

    /**
     * Nastaví název typu výstupu.
     * @param name název typu výstupu.
     */
    void setName(String name);

    /**
     * 
     * @return kód typu výstupu
     */
    String getCode();

    /**
     * Nastaví kód typu výstupu.
     * @param code kód typu výstupu
     */
    void setCode(String code);
}
