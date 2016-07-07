package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Verze pojmenovaného výstupu z archivního výstupu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
public interface ArrOutput<NO extends ArrOutputDefinition, C extends ArrChange> extends Serializable {

    /**
     * @return  identifikátor entity
     */
    Integer getOutputId();

    /**
     * @param outputId  identifikátor entity
     */
    void setOutputId(Integer outputId);

    /**
     * @return výstup z archivního souboru
     */
    NO getOutputDefinition();

    /**
     * @param outputDefinition výstup z archivního souboru
     */
    void getOutputDefinition(NO outputDefinition);

    /**
     * @return změna vytvoření
     */
    C getCreateChange();

    /**
     * @param createChange změna vytvoření
     */
    void setCreateChange(C createChange);

    /**
     * @return změna uzamčení
     */
    C getLockChange();

    /**
     * @param lockChange změna uzamčení
     */
    void setLockChange(C lockChange);

}
