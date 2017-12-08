package cz.tacr.elza.print;

import java.util.Collection;
import java.util.List;

public interface NodeLoader {

    /**
     * Initializes all nodes by specified ids.
     *
     * @param nodeIds not-null
     * @return List of initialized nodes which preserve order of specified ids.
     */
    List<Node> loadNodes(Collection<NodeId> nodeIds);
}
