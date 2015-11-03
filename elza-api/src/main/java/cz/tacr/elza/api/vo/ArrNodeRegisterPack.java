package cz.tacr.elza.api.vo;

import cz.tacr.elza.api.ArrNodeRegister;

import java.util.List;

/**
 * Zapouzdření kolekcí k obsluze ukládání a mazání vazeb mezi uzlem a rejstříkovými hesly.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ArrNodeRegisterPack<ANR extends ArrNodeRegister> {

    void setSaveList(List<ANR> saveList);

    List<ANR> getSaveList();

    List<ANR> getDeleteList();

    void setDeleteList(List<ANR> deleteList);
}
