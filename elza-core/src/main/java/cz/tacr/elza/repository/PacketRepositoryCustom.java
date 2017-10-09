package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrPacket;

/**
 * Repository pro obaly.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
public interface PacketRepositoryCustom {

    /**
     * Vyhledá obal daného typu podle zadaného názvu. Vrátí seznam obalů vyhovující zadané frázi.
     * Výsledek je stránkovaný, je
     * vrácen zadaný počet záznamů od from záznamu.
     * @param searchPacket hledaný řetězec, může být null
     * @param packetTypeId typ záznamu
     * @param firstResult id prvního záznamu
     * @param maxResults max počet záznamů
     * @return
     */
    List<ArrPacket> findPacketByTextAndType(String searchPacket, Integer packetTypeId,
                                          Integer firstResult, Integer maxResults);

    /**
     * Vrátí počet obalů vyhovující zadané frázi.
     * @param searchPacket hledaný řetězec, může být null
     * @param packetTypeId typ záznamu
     * @return
     */
    long findPacketByTextAndTypeCount(String searchPacket, Integer packetTypeId);


    List<ArrPacket> findPackets(ArrFund fund, Integer limit, String text, ArrPacket.State state);

	List<ArrPacket> findPacketsBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNodes);
}
