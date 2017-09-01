package cz.tacr.elza.print;

import java.util.Collection;
import java.util.Map;

/**
 * Interface pro hromadné načítání uzlů.
 *
 * @author Martin Šlapa
 * @since 29.08.2016
 */
public interface NodeLoader {

    /**
     * Načtení uzlů podle požadovaných identifikátorů.
     *
     * @param output  výstup
     * @param nodeIds seznam identifikátorů uzlů, které načítáme
     * @return mapa - klíč identifikátor uzlu, uzel
     */
    Map<Integer, Node> loadNodes(OutputImpl output, Collection<NodeId> nodeIds);

}
