package cz.tacr.elza.service.importnodes.vo;

import java.util.Set;

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
    Set<? extends Scope> getScopes();

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
