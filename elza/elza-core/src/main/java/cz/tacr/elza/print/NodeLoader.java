package cz.tacr.elza.print;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.print.item.Item;

public interface NodeLoader {

    /**
     * Initializes all nodes by specified ids.
     *
     * @param nodeIds not-null
     * @return List of initialized nodes which preserve order of specified ids.
     */
    List<Node> loadNodes(Collection<NodeId> nodeIds);

    /**
     * Load structured items for structured object
     * 
     * @param structObjId
     *            ID of structured object
     * @return
     */
    List<Item> loadStructItems(Integer structObjId);
}
