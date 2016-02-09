package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataPacketRefRepositoryCustom {

    List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFindingAidVersion version);
}
