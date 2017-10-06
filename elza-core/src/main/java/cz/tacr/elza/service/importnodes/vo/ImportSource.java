package cz.tacr.elza.service.importnodes.vo;

import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.RegScope;

/**
 * Rozhraní zdroje pro import.
 *
 * Průchod stromem musí být typu DFS (prohledávání do hloubky).
 *
 * @since 19.07.2017
 */
public interface ImportSource {

    /**
     * @return seznam použitých scope
     */
	List<RegScope> getScopes();

    /**
     * @return seznam použitých souborů
     */
    Set<? extends File> getFiles();

    /**
     * @return seznam použitých obalů
     */
    Set<? extends Packet> getPackets();

    /**
     * @return má další uzel?
     */
    boolean hasNext();

    /**
     * @param changeDeep callback pro orientaci ve stromu (změna úrovní)
     * @return získání dalšího uzlu ze stromu
     */
    Node getNext(DeepCallback changeDeep);

}
