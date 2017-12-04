package cz.tacr.elza.print;

import java.util.Collection;
import java.util.List;

public interface NodeLoader {

    /**
     * Initializes nodes by specified ids.
     *
     * @param nodeIds not-null
     * @return List of nodes which preserve order of specified ids.
     */
    List<Node> loadNodes(Collection<NodeId> nodeIds);
}
