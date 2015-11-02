package cz.tacr.elza.api;

import java.io.Serializable;


/**
 * Přiřazení rejstříkových hesel k jednotce archivního popisu.
 */
public interface ArrNodeRegister<AN extends ArrNode, RR extends RegRecord, AC extends ArrChange> extends Serializable {

    /**
     * ID.
     * @return  id
     */
    Integer getNodeRegisterId();

    /**
     * ID
     * @param nodeRegisterId    id
     */
    void setNodeRegisterId(Integer nodeRegisterId);

    /**
     * Uzel archivního popisu.
     * @return uzel archivního popisu
     */
    AN getNode();

    /**
     * Uzel archivního popisu.
     * @param node uzel archivního popisu
     */
    void setNode(AN node);

    /**
     * Heslo rejstříku.
     * @return heslo rejstříku
     */
    RR getRecord();

    /**
     * Heslo rejstříku.
     * @param record heslo rejstříku
     */
    void setRecord(RR record);

    /**
     * Záznam o vytvoření.
     * @return záznam o vytvoření
     */
    AC getCreateChange();

    /**
     * Záznam o vytvoření.
     * @param createChange záznam o vytvoření
     */
    void setCreateChange(AC createChange);

    /**
     * Záznam o smazání.
     * @return záznam o smazání
     */
    AC getDeleteChange();

    /**
     * Záznam o smazání.
     * @param deleteChange záznam o smazání
     */
    void setDeleteChange(AC deleteChange);

}
