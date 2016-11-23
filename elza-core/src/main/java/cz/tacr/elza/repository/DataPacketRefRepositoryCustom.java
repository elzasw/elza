package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemType;

public interface DataPacketRefRepositoryCustom {

    List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulItemType> descItemTypes, ArrFundVersion version);

    List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulItemType> itemTypes, Integer changeId);

    int countInFundVersionByPacketIds(List<Integer> packetIds, ArrFundVersion version);

    List<ArrPacket> findUsePacketsByPacketIds(List<Integer> packetIds);
}
