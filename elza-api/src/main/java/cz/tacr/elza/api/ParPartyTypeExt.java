package cz.tacr.elza.api;

import java.io.Serializable;
import java.util.List;

/**
 * Externí číselník typů osob.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface ParPartyTypeExt<PS extends ParPartySubtype> extends Serializable, ParPartyType {

    List<PS> getPartySubTypeList();

    void setPartySubTypeList(List<PS> partySubTypeList);
}
