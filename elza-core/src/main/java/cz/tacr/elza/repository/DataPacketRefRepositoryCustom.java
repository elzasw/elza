package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulDescItemType;

public interface DataPacketRefRepositoryCustom {

    List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version);

    int countInFundVersionByPacketIds(List<Integer> packetIds, ArrFundVersion version);
}
