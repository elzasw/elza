package cz.tacr.elza.service.importnodes.vo;

import java.util.List;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrStructuredObject;

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
	List<ApScope> getScopes();

    /**
     * @return seznam použitých souborů
     */
	List<ArrFile> getFiles();

    /**
     * @return seznam použitých obalů
     */
    List<ArrStructuredObject> getStructuredList();

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
