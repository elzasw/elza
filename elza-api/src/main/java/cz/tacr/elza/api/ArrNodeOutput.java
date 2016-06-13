package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Vazba výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
public interface ArrNodeOutput<NO extends ArrOutputDefinition, C extends ArrChange, N extends ArrNode> extends Serializable {

    /**
     * @return  identifikátor entity
     */
    Integer getNodeOutputId();

    /**
     * @param nodeOutputId identifikátor entity
     */
    void setNodeOutputId(Integer nodeOutputId);

    /**
     * @return pojmenovaný výstup z archivního souboru
     */
    NO getOutputDefinition();

    /**
     * @param outputDefinition pojmenovaný výstup z archivního souboru
     */
    void setOutputDefinition(NO outputDefinition);

    /**
     * @return navázaný uzel
     */
    N getNode();

    /**
     * @param node navázaný uzel
     */
    void setNode(N node);

    /**
     * @return změna vytvoření
     */
    C getCreateChange();

    /**
     * @param createChange změna vytvoření
     */
    void setCreateChange(C createChange);

    /**
     * @return změna smazání
     */
    C getDeleteChange();

    /**
     * @param deleteChange změna smazání
     */
    void setDeleteChange(C deleteChange);
}
