package cz.tacr.elza.api;

import java.io.Serializable;

/**
 * Vazba výstupu na podstromy archivního popisu.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
public interface ArrNodeOutput<O extends ArrOutput, C extends ArrChange, N extends ArrNode> extends Serializable {

    /**
     * @return  identifikátor entity
     */
    Integer getNodeOutputId();

    /**
     * @param nodeOutputId identifikátor entity
     */
    void setNodeOutputId(Integer nodeOutputId);

    /**
     * @return verze pojmenovaného výstupu z archivního výstupu
     */
    O getOutput();

    /**
     * @param output verze pojmenovaného výstupu z archivního výstupu
     */
    void setOutput(O output);

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
