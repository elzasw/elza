package cz.tacr.elza.print;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Iterator;
import java.util.List;

/**
 * Basic referenced node implementation for output tree with minimal memory footprint.
 */
public class RefNodeId extends NodeId {

    public RefNodeId(final int nodeId) {
        super(nodeId, null, -1, -1, true);
    }

    @Override
    public NodeId getParent() {
        throw new NotImplementedException("Nelze zjistit nadřízená JP, protože se jedná o odkazovou JP");
    }

    @Override
    public int getPosition() {
        throw new NotImplementedException("Nelze zjistit pozici JP, protože se jedná o odkazovou JP");
    }

    @Override
    public int getDepth() {
        throw new NotImplementedException("Nelze zjistit úroveň JP, protože se jedná o odkazovou JP");
    }

    @Override
    public List<NodeId> getChildren() {
        throw new NotImplementedException("Nelze zjistit podřízené JP, protože se jedná o odkazovou JP");
    }

    @Override
    public Iterator<NodeId> getIteratorDFS() {
        throw new NotImplementedException("Nelze iterovat JP do hloubky, protože se jedná o odkazovou JP");
    }

    @Override
    void addChild(final NodeId child) {
        throw new NotImplementedException("Nelze přidat podřízenou JP, protože se jedná o odkazovou JP");
    }

}
