package cz.tacr.elza.domain.vo;

import java.util.List;

import cz.tacr.elza.domain.ArrNodeRegister;

/**
 * Zapouzdření kolekcí k obsluze ukládání a mazání vazeb mezi uzlem a rejstříkovými hesly.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public class ArrNodeRegisterPack {

    /**
     * K vytvoření či modifikaci.
     */
    private List<ArrNodeRegister> saveList;

    /**
     * Ke smazání.
     */
    private List<ArrNodeRegister> deleteList;


    /**
     * Default pro JSON operace.
     */
    public ArrNodeRegisterPack() {
    }

    /**
     * Konstruktor pro snažší použití.
     *
     * @param saveList      k uložení/update
     * @param deleteList    ke smazání
     */
    public ArrNodeRegisterPack(final List<ArrNodeRegister> saveList, final List<ArrNodeRegister> deleteList) {
        this.saveList = saveList;
        this.deleteList = deleteList;
    }

    /**
     * List pro uložení/modifikaci.
     * @return  list pro uložení/modifikaci
     */
    public List<ArrNodeRegister> getSaveList() {
        return saveList;
    }

    /**
     * List pro uložení/modifikaci.
     * @param saveList  list pro uložení/modifikaci
     */
    public void setSaveList(final List<ArrNodeRegister> saveList) {
        this.saveList = saveList;
    }

    /**
     * List pro smazání.
     * @return      list pro smazání
     */
    public List<ArrNodeRegister> getDeleteList() {
        return deleteList;
    }

    /**
     * List pro smazání.
     * @param deleteList    list pro smazání
     */
    public void setDeleteList(final List<ArrNodeRegister> deleteList) {
        this.deleteList = deleteList;
    }
}
