package cz.tacr.elza.api;

import java.io.Serializable;
import java.util.List;

/**
 * Externí číselník typů osob. Typ osoby s přidanou vazbou na jeho podtypy.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyTypeExt extends Serializable, ParPartyType {

//    /**
//     * Podtypy daného typu.
//     * @return  množina objekty podtypů, může být prázdná
//     */
//    List<PS> getPartySubTypeList();
//
//    /**
//     * Podtypy daného typu.
//     * @param partySubTypeList  množina objekty podtypů, může být prázdná
//     */
//    void setPartySubTypeList(List<PS> partySubTypeList);
}
